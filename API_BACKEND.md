# LibreCellar - Backend API

The REST API the LibreCellar Android app expects from its server: the client's structure, the endpoints with pagination, the connection test and the JSON data contracts. The LibreCellar server implements this API.

---

## 1. Architectural Overview & Mental Model

In this Kotlin Jetpack Compose Android app, all backend communication is cleanly separated from the UI:

```
app/src/main/java/org/librecellar/data/
├── api/
│   └── WineApiService.kt      <-- Retrofit HTTP Interface (routes, methods, query parameters)
├── model/
│   └── WineModels.kt          <-- DTOs & JSON serialization (@SerialName snake_case mapping, kotlinx.serialization)
└── repository/
    └── WineRepository.kt      <-- Network engine (OkHttp client, login token, Coroutines, StateFlows, Sync)
```

### Key Libraries & Patterns:
- **Retrofit 3**: Declarative HTTP client. Translates Kotlin interfaces into HTTP calls.
- **kotlinx.serialization**: Kotlin's JSON library (a compiler plugin, no reflection) mapping `snake_case` keys to Kotlin data class properties via Retrofit's official converter. The shared settings in `data/api/ApiJson.kt` ignore unknown fields, leave out `null` values and send default values.
- **OkHttp 5**: The underlying HTTP client engine managing connection pooling, timeouts, and interceptors (such as attaching the login token).
- **Kotlin Coroutines (`Dispatchers.IO`)**: Asynchronous network tasks run off the main Android UI thread.
- **Jetpack DataStore**: Stores the app settings (server URL, username, filters) and the encrypted login token.
- **StateFlow / Repository Pattern**: When network calls succeed, they update observable state flows (`wines`, `locations`, `suppliers`). If the backend is unreachable or offline, the app displays local state.

---

## 2. REST API Specification

### Authentication & Headers
- **Authentication**: the app logs in once with `POST /api/auth/login` (`{"username", "password"}`) and then sends `Authorization: Bearer <token>` with every request; the token identifies the user, no other identity header is sent. The password is **never stored** on the device: only the token is kept, encrypted with an Android Keystore key (in Jetpack DataStore). A `401` means the login expired or was revoked (e.g. after a password reset), and the user logs in again. `POST /api/auth/logout` ends the login. Backends also accept HTTP Basic Auth (`Authorization: Basic <base64(username:password)>`) for scripts and `curl`.
- **Logging**: the app logs HTTP requests in debug builds only, without bodies and with `Authorization`/`Cookie` headers redacted.
- **Passwords** are stored as argon2 hashes on the server.
- **Content-Type**: `application/json`
- **Accept**: `application/json`

*(In the app's **Settings** tab, users configure Server URL, Username, and Password, and tap **Save Settings & Test Connection** to log in, verify connectivity and authentication against protected endpoints, and load the collection from the server. The password field is cleared afterwards; leaving it empty keeps the current login).*

### Users, Collections & Sharing
The LibreCellar server is multi-user and built for households sharing one collection:
- Every wine, cellar location (grid), merchant and bottle picture belongs to one user's collection (its **owner**).
- An owner can share their whole collection with other users as **read-only** (`read`) or **read/write** (`write`).
- **Merged view:** list endpoints return the user's own collection *merged with every collection shared with them*. The app needs no collection selection; shared entries appear like own ones.
- Every returned wine, location and merchant carries two extra fields: `owner` (username of the collection owner) and `access` (`owner`, `write` or `read`). The app marks `read` entries unobtrusively, blocks editing them, and offers a persisted filter to hide read-only and/or read-write shared wines. Clients send these fields back unchanged; the backend ignores them on input.
- **Where new entries go:** a wine placed in a rack joins the rack's collection; anything else goes to the user's *default collection*. When a collection is shared read/write with a user who has no entries of their own yet, it becomes their default automatically (a household member joining); otherwise the default is their own collection. Users change it in the app's **Settings** ("New Entries Go To"), on the web app's Sharing page, or via `PUT /api/me/default-collection`. An explicit `owner=<username>` query parameter overrides all of this.
- Updates and deletes always stay in the object's collection: allowed with `owner`/`write` access, `403` with `read` access, `404` without access. A wine can only be placed in a rack of the same collection.
- Optional `owner=<username>` on list endpoints narrows the view to that one collection.

---

### Endpoint Reference & Pagination Contract

All `GET` collection endpoints (`/api/wines`, `/api/cellar/locations`, `/api/suppliers`, and their aliases `/api/racks` and `/api/merchants`) **support pagination with a standard page limit of 20 items per page**:
- `page`: Integer (1-indexed, default: `1`, minimum: `1`).
- `limit`: Integer (default: `20`, range: `1` to `100`).
- Offset calculation: `offset = (page - 1) * limit`.

#### 1. System & Health
| Method | Endpoint | Description | Protected | Response |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Service health check | No | `{"status": "ok", "app": "LibreCellar Server"}` |

#### 2. Wines (`/api/wines`)
| Method | Endpoint | Description | Query Params | Protected | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/wines` | List wines (paginated) | `page=1&limit=20` | Yes | `List<Wine>` (max 20 items) |
| `GET` | `/api/wines/{id}` | Get single wine by ID | None | Yes | `Wine` (or 404) |
| `GET` | `/api/wines/barcode/{barcode}` | Lookup by bottle barcode | None | Yes | `Wine` (or 404) |
| `POST` | `/api/wines` | Create a new wine entry | None | Yes | `Wine` (created object, 201) |
| `PUT` | `/api/wines/{id}` | Update existing wine | None | Yes | `Wine` (updated object) |
| `DELETE`| `/api/wines/{id}` | Delete wine entry (also deletes its picture once unused) | None | Yes | `204 No Content` |
| `POST` | `/api/wines/{id}/increment` | Increment bottle stock by 1 (atomic) | None | Yes | `DecrementInventoryResponse` |
| `POST` | `/api/wines/{id}/decrement` | Decrement bottle stock by 1 (atomic; the last bottle also frees its rack slot) | None | Yes | `DecrementInventoryResponse` |

#### 3. Cellar Locations & Racks (`/api/cellar/locations` & `/api/racks`)
| Method | Endpoint | Description | Query Params | Protected | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/cellar/locations` | List cellar racks (paginated) | `page=1&limit=20` | Yes | `List<CellarLocation>` (max 20) |
| `GET` | `/api/racks` | Alias for racks | `page=1&limit=20` | Yes | `List<CellarLocation>` (max 20) |
| `GET` | `/api/cellar/locations/{id}`| Get rack by ID | None | Yes | `CellarLocation` (or 404) |
| `POST` | `/api/cellar/locations` | Create a new rack | None | Yes | `CellarLocation` (201) |
| `PUT` | `/api/cellar/locations/{id}`| Update existing rack (shrinking frees slots that no longer exist) | None | Yes | `CellarLocation` |
| `DELETE`| `/api/cellar/locations/{id}`| Delete rack (its bottles become unassigned) | None | Yes | `204 No Content` |
| `POST` | `/api/cellar/locations/{id}/assign-slot` | Assign wine to a rack slot; `location_id: null` takes it out of its rack | None | Yes | Updated `Wine` object |

#### 4. Merchants & Suppliers (`/api/suppliers` & `/api/merchants`)
| Method | Endpoint | Description | Query Params | Protected | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/suppliers` | List merchants (paginated) | `page=1&limit=20` | Yes | `List<Supplier>` (max 20) |
| `GET` | `/api/merchants` | Alias for merchants | `page=1&limit=20` | Yes | `List<Supplier>` (max 20) |
| `GET` | `/api/suppliers/{id}` | Get merchant by ID | None | Yes | `Supplier` (or 404) |
| `POST` | `/api/suppliers` | Add a merchant | None | Yes | `Supplier` (201) |
| `PUT` | `/api/suppliers/{id}` | Update merchant | None | Yes | `Supplier` |
| `DELETE`| `/api/suppliers/{id}` | Delete merchant | None | Yes | `204 No Content` |

#### 5. Bottle Pictures & Image Storage (`/api/images`)
| Method | Endpoint | Description | Query Params | Protected | Response |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/images/upload` | Upload bottle picture (multipart, field `file`) | `owner` (optional) | Yes | `{"filename": "UUIDv7.jpg", "url": "/api/images/UUIDv7.jpg"}` |
| `GET` | `/api/images/{filename}` | Retrieve picture (stream) | None | Yes | Image binary stream (e.g. `image/jpeg`) |
| `DELETE`| `/api/images/{filename}` | Delete picture | None | Yes | `204 No Content` |

*Storage Architecture*: The server stores uploaded bottle images in a configurable path (`LIBRECELLAR_IMAGE_DIR`). Inside this path, each collection owner has a subfolder; the owner is determined from the authenticated user (or the `owner` parameter on upload). Each file is renamed to a time-ordered UUIDv7 (`UUIDv7.<extension>`). The database stores **only** the filename (`wine.photo_url = "UUIDv7.jpg"`); access to a picture follows access to the owner's collection.

#### 6. Current User, Collections & Sharing
| Method | Endpoint | Description | Protected | Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Login (Android and web app) | No | `{"token": "...", "expires_at": "...", "user": UserInfo}` (body `{"username", "password"}`) |
| `POST` | `/api/auth/logout` | End a web app login | Bearer | `204 No Content` |
| `GET` | `/api/me` | Authenticated user's profile | Yes | `{"username": "bob", "is_admin": false, "default_collection": "alice", "first_name": "Bob", "last_name": null, "email": "bob@example.org", "created_at": "2026-10-21T08:00:00Z", "last_activity_at": "..."}` |
| `PUT` | `/api/me` | Update own first name, last name, e-mail | Yes | `UserInfo` |
| `DELETE`| `/api/me` | Delete own account and everything related to it | Yes | `204 No Content` (body `{"password"}`) |
| `PUT` | `/api/me/default-collection` | Collection new entries go to (own or shared read/write) | Yes | `UserInfo` (body `{"owner": "alice"}`, `null` = own) |
| `PUT` | `/api/me/password` | Change own password | Yes | `204 No Content` (body `{"current_password", "new_password"}`) |
| `GET` | `/api/collections` | Collections accessible to the user | Yes | `[{"owner": "alice", "permission": "owner"}, {"owner": "bob", "permission": "read"}]` |
| `DELETE`| `/api/collections/{owner}` | Leave a collection shared with you | Yes | `204 No Content` |
| `GET` | `/api/users/search?q=<prefix>` | Username autocomplete (usernames only) | Yes | `["bob", "bobby"]` |
| `GET` | `/api/shares` | Users you shared your collection with | Yes | `[{"username": "bob", "permission": "read", "created_at": "..."}]` |
| `PUT` | `/api/shares/{username}` | Invite user / change permission | Yes | `Share` (body `{"permission": "read" \| "write"}`) |
| `DELETE`| `/api/shares/{username}` | Revoke access | Yes | `204 No Content` |

#### 7. User Administration (admins only)
| Method | Endpoint | Description | Response |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admin/users` | All users with stats | `[{"username", "first_name", "last_name", "email", "is_admin", "is_active", "created_at", "last_activity_at", "stats": {"wines", "bottles", "racks", "merchants"}}]` |
| `POST` | `/api/admin/users` | Create user | `AdminUser` (body `{"username", "password", "is_admin", "first_name", "last_name", "email"}`) |
| `GET` | `/api/admin/users/{username}` | One user | `AdminUser` |
| `PUT` | `/api/admin/users/{username}` | Edit names, e-mail, admin role, enabled | `AdminUser` |
| `PUT` | `/api/admin/users/{username}/password` | Reset to a password the admin enters | `204 No Content` (body `{"new_password"}`) |
| `DELETE`| `/api/admin/users/{username}` | Delete user and all their data | `204 No Content` |

All timestamps are UTC (ISO 8601). Users are created by an administrator (above, or in the web app served by the backend at `/`) or on the server with `python -m backend.cli create-user <name>`; sharing, profiles and administration are also available in that web app.

---

## 3. Connection Test & Data Ownership

**The backend manages all data.** The app keeps no data of its own: every change (add, edit, delete, tick off a
bottle, place a bottle in a rack, ...) is sent as a REST request, and once the server accepted it the app reloads the
affected lists from the server to update its view. The app never applies changes locally, not even when offline:
a failed request is shown to the user and nothing changes. Server-side consequences (freeing a rack slot when the last
bottle is gone, unassigning bottles of a deleted rack, removing unused pictures, ...) are handled by the backend only.

When a user taps **Save Settings & Test Connection** in the LibreCellar Android app:

```
[Android App]
      │
      ├── 0. POST /api/auth/login (only if a password was entered)
      │      └── Stores the returned token (encrypted), discards the password
      │      └── If 401: STOP -> "Wrong username or password"
      │
      ├── 1. GET /api/health (Ping test)
      │      └── If fails: STOP -> "Server health check failed"
      │
      ├── 2. GET /api/suppliers?page=1&limit=20 (Protected Endpoint Check)
      │      └── Checks the login token (Authorization: Bearer)
      │      └── If 401/403/fails: STOP -> DO NOT show connected
      │
      └── 3. Protected endpoint succeeded! Backend is marked ONLINE
             └── Load wines, racks and merchants from the server with pagination (limit=20)
             └── Load /api/me and /api/collections (multi-user backend)
             └── Display success with the number of loaded records
```

This prevents false positives where an unauthenticated or misconfigured server might respond to `/api/health` but reject actual cellar data access.

---

## 4. JSON Schemas & Data Contracts

### Wine Object (`Wine`)
```json
{
  "id": "c1f7b029-798c-443b-873b-e0f3408092ec",
  "name": "Château Belle Rive",
  "vintage": 2015,
  "country": "France",
  "region": "Bordeaux",
  "varietal": "Cabernet Sauvignon Blend",
  "producer": "Château Belle Rive",
  "wine_type": "RED",
  "purchase_date": "2021-09-15",
  "alcohol_percent": 13.5,
  "size": "0.75L",
  "tasting_rating": 5,
  "comments": "Decanted 2 hours, silky tannins, extraordinary finish.",
  "purchase_price": 750.00,
  "currency": "EUR",
  "purchase_location": "Cave du Village",
  "photo_url": null,
  "barcode": "3421580001015",
  "price_tier": "VERY_EXPENSIVE",
  "quantity": 3,
  "location_id": "loc-rack-a",
  "slot_row": 0,
  "slot_column": 1,
  "slot_label": "Row 1, Slot 2",
  "owner": "alice",
  "access": "write"
}
```
*`currency` is the ISO 4217 code of `purchase_price` (e.g. `"EUR"`, `"GBP"`). The app sets it on every wine it saves (default: the device region's currency, changeable per wine); wines saved before currencies existed have `null` and are shown in the viewer's device currency. Prices are never converted between currencies; price tiers only compare wines of the same currency.*

*`owner` and `access` are returned by the multi-user backend (also on `CellarLocation` and `Supplier`) and are read-only.*

### Cellar Location / Rack Object (`CellarLocation`)
```json
{
  "id": "loc-rack-a",
  "name": "Grand Cru Rack A",
  "rows": 4,
  "columns": 6,
  "description": "Shelving unit in lower cellar",
  "temperature_celsius": 13.0,
  "humidity_percent": 70.0,
  "color_hex": "#6B1D2F"
}
```
*Note: `temperature_celsius` and `humidity_percent` are optional (`null` if unmonitored).*

### Merchant / Supplier Object (`Supplier`)
```json
{
  "id": "sup-1",
  "name": "Cave du Village",
  "contact_name": "Anne Martin",
  "email": "orders@example.com",
  "phone": "+352 000 000",
  "website": "https://shop.example.com",
  "city": "Remich",
  "country": "Luxembourg",
  "notes": "Primary supplier for Bordeaux futures and allocations.",
  "type": "Wine Merchant",
  "date_added": "2024-05-01"
}
```
*`type` is one of the app's merchant types (default `"Wine Merchant"`). `date_added` is an ISO date; the server sets it to the current date when a merchant is created without one, and older merchants may have `null`. A client that sends neither field (e.g. an older app version) leaves both unchanged on update.*

### Assign Slot Request (`AssignSlotRequest`)
```json
{
  "wine_id": "c1f7b029-798c-443b-873b-e0f3408092ec",
  "location_id": "loc-rack-a",
  "slot_row": 0,
  "slot_column": 1
}
```
*`location_id` may be `null` to take the wine out of its rack.*

### Decrement Inventory Response (`DecrementInventoryResponse`)
```json
{
  "wine_id": "c1f7b029-798c-443b-873b-e0f3408092ec",
  "previous_quantity": 3,
  "remaining_quantity": 2,
  "message": "Stock quantity decremented"
}
```

---
## 5. How to Adapt the Kotlin Code

If you extend or modify endpoints in your backend, here is how and where to adapt the Kotlin application:

### Step 1: Updating the Retrofit Interface (`WineApiService.kt`)
Add or update the HTTP method, path, and query annotations:
```kotlin
@GET("api/wines")
suspend fun getWines(
    @Query("page") page: Int? = null,
    @Query("limit") limit: Int? = null
): Response<List<Wine>>
```

### Step 2: Updating Data Models (`WineModels.kt`)
kotlinx.serialization maps JSON fields via `@SerialName("snake_case_key")`:
```kotlin
@Serializable
data class Wine(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("custom_field") val customField: String? = null
)
```
New fields the backend adds are ignored by older app versions, and `null` values are not sent (see `ApiJson`).

### Step 3: Updating the Repository (`WineRepository.kt`)
Update the coroutine network caller in `WineRepository.kt`:
```kotlin
suspend fun fetchWinesPage(page: Int, limit: Int = 20): List<Wine> = withContext(Dispatchers.IO) {
    val resp = apiService.getWines(page = page, limit = limit)
    if (resp.isSuccessful) resp.body().orEmpty() else emptyList()
}
```

### Step 4: Updating the UI (Compose Screen)
Collect the `StateFlow` from the repository inside the Composable:
```kotlin
val wines by repository.wines.collectAsState()
```
Compose will automatically recompose the UI when the data updates.
