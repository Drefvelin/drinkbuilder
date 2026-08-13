package net.tfminecraft.DrinkBuilder.api;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.tfminecraft.DrinkBuilder.Cache;

/**
 * HTTP client for ProvinceSystem drink plugin routes.
 */
public final class ProvinceSystemClient {

	private static final int TIMEOUT_MS = 8000;
	private static final int DOWNLOAD_TIMEOUT_MS = 30000;
	private static final Gson GSON = new Gson();

	private ProvinceSystemClient() {}

	public static final class SimpleResult {
		public final boolean ok;
		public final String error;
		public final String body;

		private SimpleResult(boolean ok, String error, String body) {
			this.ok = ok;
			this.error = error;
			this.body = body;
		}

		public static SimpleResult success(String body) {
			return new SimpleResult(true, null, body);
		}

		public static SimpleResult fail(String error) {
			return new SimpleResult(false, error, null);
		}
	}

	public static final class CatalogPushResult {
		public final boolean ok;
		public final int ingredients;
		public final String updatedAt;
		public final String error;

		private CatalogPushResult(boolean ok, int ingredients, String updatedAt, String error) {
			this.ok = ok;
			this.ingredients = ingredients;
			this.updatedAt = updatedAt;
			this.error = error;
		}

		public static CatalogPushResult success(int ingredients, String updatedAt) {
			return new CatalogPushResult(true, ingredients, updatedAt, null);
		}

		public static CatalogPushResult fail(String error) {
			return new CatalogPushResult(false, 0, null, error);
		}
	}

	public static final class DownloadResult {
		public final boolean ok;
		public final byte[] data;
		public final String error;

		private DownloadResult(boolean ok, byte[] data, String error) {
			this.ok = ok;
			this.data = data;
			this.error = error;
		}

		public static DownloadResult success(byte[] data) {
			return new DownloadResult(true, data, null);
		}

		public static DownloadResult fail(String error) {
			return new DownloadResult(false, null, error);
		}
	}

	public static final class AppliedResult {
		public final boolean ok;
		public final List<String> applied;
		public final String error;

		private AppliedResult(boolean ok, List<String> applied, String error) {
			this.ok = ok;
			this.applied = applied == null
				? Collections.emptyList()
				: Collections.unmodifiableList(applied);
			this.error = error;
		}

		public static AppliedResult success(List<String> applied) {
			return new AppliedResult(true, applied, null);
		}

		public static AppliedResult fail(String error) {
			return new AppliedResult(false, Collections.emptyList(), error);
		}
	}

	public static final class TextureInfo {
		public final String id;
		public final Integer cmd;
		public final String iaItemId;
		public final String pngPath;

		public TextureInfo(String id, Integer cmd, String iaItemId, String pngPath) {
			this.id = id;
			this.cmd = cmd;
			this.iaItemId = iaItemId;
			this.pngPath = pngPath;
		}
	}

	/** One drink from GET /drinks/plugin/pending-apply. */
	public static final class PendingDrink {
		public final String id;
		public final String playerUuid;
		public final String slug;
		public final String displayName;
		public final String status;
		public final boolean newTexture;
		public final String textureId;
		public final Map<String, Object> recipe;
		public final List<String> files;
		public final TextureInfo texture;

		public PendingDrink(
			String id,
			String playerUuid,
			String slug,
			String displayName,
			String status,
			boolean newTexture,
			String textureId,
			Map<String, Object> recipe,
			List<String> files,
			TextureInfo texture
		) {
			this.id = id;
			this.playerUuid = playerUuid;
			this.slug = slug;
			this.displayName = displayName;
			this.status = status;
			this.newTexture = newTexture;
			this.textureId = textureId;
			this.recipe = recipe == null ? Map.of() : recipe;
			this.files = files == null ? List.of() : List.copyOf(files);
			this.texture = texture;
		}

		public boolean needsIaWrite() {
			if (textureId == null || textureId.isBlank()) {
				return false;
			}
			if (texture != null && texture.cmd != null) {
				return false;
			}
			return true;
		}

		public Integer existingCmd() {
			return texture == null ? null : texture.cmd;
		}
	}

	public static final class ListResult {
		public final boolean ok;
		public final List<PendingDrink> submissions;
		public final String error;

		private ListResult(boolean ok, List<PendingDrink> submissions, String error) {
			this.ok = ok;
			this.submissions = submissions == null
				? Collections.emptyList()
				: Collections.unmodifiableList(submissions);
			this.error = error;
		}

		public static ListResult success(List<PendingDrink> submissions) {
			return new ListResult(true, submissions, null);
		}

		public static ListResult fail(String error) {
			return new ListResult(false, Collections.emptyList(), error);
		}
	}

	public static CatalogPushResult pushCatalog(String jsonBody) {
		SimpleResult raw = putJson("/drinks/plugin/catalog", jsonBody);
		if (!raw.ok) {
			return CatalogPushResult.fail(raw.error);
		}
		String body = raw.body == null ? "" : raw.body;
		int count = Cache.ingredients == null ? 0 : Cache.ingredients.size();
		String updatedAt = jsonString(body, "updated_at");
		return CatalogPushResult.success(count, updatedAt);
	}

	public static SimpleResult pushPlayerMeta(String jsonBody) {
		return putJson("/drinks/plugin/player-meta", jsonBody);
	}

	/**
	 * Upload a drink creator asset PNG (glass_bottle.png / potion_overlay.png).
	 */
	public static SimpleResult putDrinkAsset(String fileName, byte[] pngBytes) {
		String name = fileName == null ? "" : fileName.trim();
		if (name.isEmpty() || pngBytes == null || pngBytes.length == 0) {
			return SimpleResult.fail("asset name and PNG bytes required");
		}
		return putBytes("/drinks/plugin/assets/" + name, pngBytes, "image/png");
	}

	public static ListResult listPendingApply() {
		SimpleResult raw = getJson("/drinks/plugin/pending-apply");
		if (!raw.ok) {
			return ListResult.fail(raw.error);
		}
		try {
			return ListResult.success(parsePendingDrinks(raw.body));
		} catch (Exception e) {
			return ListResult.fail("Bad pending-apply payload: " + e.getMessage());
		}
	}

	public static DownloadResult downloadSubmissionFile(String submissionId, String filename) {
		String id = submissionId == null ? "" : submissionId.trim();
		String name = filename == null ? "" : filename.trim();
		if (id.isEmpty() || name.isEmpty()) {
			return DownloadResult.fail("submission id and filename are required");
		}
		if (name.contains("..") || name.contains("/") || name.contains("\\")) {
			return DownloadResult.fail("invalid filename");
		}
		String path = "/drinks/plugin/submissions/"
			+ id
			+ "/files/"
			+ java.net.URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
		return getBytes(path);
	}

	public static SimpleResult assignTextureCmd(String textureId, int cmd, String iaItemId) {
		String tid = textureId == null ? "" : textureId.trim();
		if (tid.isEmpty()) {
			return SimpleResult.fail("texture_id is required");
		}
		String ia = iaItemId == null ? "" : iaItemId.trim();
		if (ia.isEmpty()) {
			return SimpleResult.fail("ia_item_id is required");
		}
		String body = "{\"cmd\":" + cmd + ",\"ia_item_id\":\"" + escapeJson(ia) + "\"}";
		return postJson("/drinks/plugin/textures/" + tid + "/cmd", body);
	}

	public static AppliedResult markApplied(List<String> submissionIds) {
		if (submissionIds == null || submissionIds.isEmpty()) {
			return AppliedResult.success(Collections.emptyList());
		}
		StringBuilder sb = new StringBuilder("{\"submission_ids\":[");
		boolean first = true;
		for (String id : submissionIds) {
			if (id == null || id.isBlank()) {
				continue;
			}
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append('"').append(escapeJson(id.trim())).append('"');
		}
		sb.append("]}");
		if (first) {
			return AppliedResult.success(Collections.emptyList());
		}
		SimpleResult raw = postJson("/drinks/plugin/applied", sb.toString());
		if (!raw.ok) {
			return AppliedResult.fail(raw.error);
		}
		List<String> applied = parseStringArray(raw.body, "applied");
		return AppliedResult.success(applied);
	}

	public static final class DeletableDrink {
		public final String id;
		public final String displayName;
		public final String status;

		public DeletableDrink(String id, String displayName, String status) {
			this.id = id;
			this.displayName = displayName;
			this.status = status;
		}
	}

	public static final class DeletableListResult {
		public final boolean ok;
		public final List<DeletableDrink> drinks;
		public final String error;

		private DeletableListResult(boolean ok, List<DeletableDrink> drinks, String error) {
			this.ok = ok;
			this.drinks = drinks == null
				? Collections.emptyList()
				: Collections.unmodifiableList(drinks);
			this.error = error;
		}

		public static DeletableListResult success(List<DeletableDrink> drinks) {
			return new DeletableListResult(true, drinks, null);
		}

		public static DeletableListResult fail(String error) {
			return new DeletableListResult(false, Collections.emptyList(), error);
		}

		public List<String> ids() {
			List<String> out = new ArrayList<>();
			for (DeletableDrink d : drinks) {
				if (d != null && d.id != null && !d.id.isBlank()) {
					out.add(d.id);
				}
			}
			return out;
		}
	}

	public static final class DrinkGetResult {
		public final boolean ok;
		public final PendingDrink drink;
		public final String error;

		private DrinkGetResult(boolean ok, PendingDrink drink, String error) {
			this.ok = ok;
			this.drink = drink;
			this.error = error;
		}

		public static DrinkGetResult success(PendingDrink drink) {
			return new DrinkGetResult(true, drink, null);
		}

		public static DrinkGetResult fail(String error) {
			return new DrinkGetResult(false, null, error);
		}
	}

	public static final class RevokeResult {
		public final boolean ok;
		public final boolean deleted;
		public final boolean textureFreed;
		public final String textureId;
		public final String iaItemId;
		public final Integer cmd;
		public final String error;

		private RevokeResult(
			boolean ok,
			boolean deleted,
			boolean textureFreed,
			String textureId,
			String iaItemId,
			Integer cmd,
			String error
		) {
			this.ok = ok;
			this.deleted = deleted;
			this.textureFreed = textureFreed;
			this.textureId = textureId;
			this.iaItemId = iaItemId;
			this.cmd = cmd;
			this.error = error;
		}

		public static RevokeResult success(
			boolean textureFreed,
			String textureId,
			String iaItemId,
			Integer cmd
		) {
			return new RevokeResult(
				true, true, textureFreed, textureId, iaItemId, cmd, null
			);
		}

		public static RevokeResult fail(String error) {
			return new RevokeResult(false, false, false, null, null, null, error);
		}
	}

	public static DeletableListResult listDeletableDrinks() {
		SimpleResult raw = getJson("/drinks/plugin/drinks/deletable");
		if (!raw.ok) {
			return DeletableListResult.fail(raw.error);
		}
		try {
			List<DeletableDrink> out = new ArrayList<>();
			JsonObject root = JsonParser.parseString(raw.body == null ? "{}" : raw.body)
				.getAsJsonObject();
			JsonArray arr = root.getAsJsonArray("drinks");
			if (arr != null) {
				for (JsonElement el : arr) {
					if (el == null || !el.isJsonObject()) {
						continue;
					}
					JsonObject obj = el.getAsJsonObject();
					String id = asString(obj, "id");
					if (id == null || id.isBlank()) {
						continue;
					}
					out.add(new DeletableDrink(
						id,
						asString(obj, "display_name"),
						asString(obj, "status")
					));
				}
			}
			return DeletableListResult.success(out);
		} catch (Exception e) {
			return DeletableListResult.fail("Bad deletable payload: " + e.getMessage());
		}
	}

	public static DrinkGetResult getDrink(String submissionId) {
		String id = submissionId == null ? "" : submissionId.trim();
		if (id.isEmpty()) {
			return DrinkGetResult.fail("submission id required");
		}
		SimpleResult raw = getJson("/drinks/plugin/drinks/" + id);
		if (!raw.ok) {
			return DrinkGetResult.fail(raw.error);
		}
		try {
			List<PendingDrink> parsed = parsePendingDrinks(
				"{\"submissions\":[" + raw.body + "]}"
			);
			if (parsed.isEmpty()) {
				return DrinkGetResult.fail("Bad drink payload");
			}
			return DrinkGetResult.success(parsed.get(0));
		} catch (Exception e) {
			return DrinkGetResult.fail("Bad drink payload: " + e.getMessage());
		}
	}

	public static RevokeResult revokeDrink(String submissionId) {
		String id = submissionId == null ? "" : submissionId.trim();
		if (id.isEmpty()) {
			return RevokeResult.fail("submission id required");
		}
		SimpleResult raw = postJson("/drinks/plugin/drinks/" + id + "/revoke", "{}");
		if (!raw.ok) {
			return RevokeResult.fail(raw.error);
		}
		try {
			JsonObject obj = JsonParser.parseString(raw.body == null ? "{}" : raw.body)
				.getAsJsonObject();
			boolean freed = obj.has("texture_freed")
				&& !obj.get("texture_freed").isJsonNull()
				&& obj.get("texture_freed").getAsBoolean();
			Integer cmd = null;
			if (obj.has("cmd") && !obj.get("cmd").isJsonNull()) {
				cmd = obj.get("cmd").getAsInt();
			}
			return RevokeResult.success(
				freed,
				asString(obj, "texture_id"),
				asString(obj, "ia_item_id"),
				cmd
			);
		} catch (Exception e) {
			return RevokeResult.fail("Bad revoke payload: " + e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	static List<PendingDrink> parsePendingDrinks(String json) {
		List<PendingDrink> out = new ArrayList<>();
		if (json == null || json.isBlank()) {
			return out;
		}
		JsonObject root = JsonParser.parseString(json).getAsJsonObject();
		JsonArray arr = root.getAsJsonArray("submissions");
		if (arr == null) {
			return out;
		}
		for (JsonElement el : arr) {
			if (el == null || !el.isJsonObject()) {
				continue;
			}
			JsonObject obj = el.getAsJsonObject();
			String id = asString(obj, "id");
			if (id == null || id.isBlank()) {
				continue;
			}
			Map<String, Object> recipe = Map.of();
			if (obj.has("recipe") && obj.get("recipe").isJsonObject()) {
				recipe = GSON.fromJson(obj.get("recipe"), Map.class);
			}
			List<String> files = new ArrayList<>();
			if (obj.has("files") && obj.get("files").isJsonArray()) {
				for (JsonElement f : obj.getAsJsonArray("files")) {
					if (f != null && f.isJsonPrimitive()) {
						files.add(f.getAsString());
					}
				}
			}
			TextureInfo texture = null;
			if (obj.has("texture") && obj.get("texture").isJsonObject()) {
				JsonObject t = obj.getAsJsonObject("texture");
				Integer cmd = null;
				if (t.has("cmd") && !t.get("cmd").isJsonNull()) {
					cmd = t.get("cmd").getAsInt();
				}
				texture = new TextureInfo(
					asString(t, "id"),
					cmd,
					asString(t, "ia_item_id"),
					asString(t, "png_path")
				);
			}
			boolean newTexture = false;
			if (obj.has("new_texture") && !obj.get("new_texture").isJsonNull()) {
				JsonElement nt = obj.get("new_texture");
				if (nt.isJsonPrimitive() && nt.getAsJsonPrimitive().isBoolean()) {
					newTexture = nt.getAsBoolean();
				} else if (nt.isJsonPrimitive() && nt.getAsJsonPrimitive().isNumber()) {
					newTexture = nt.getAsInt() != 0;
				}
			}
			out.add(new PendingDrink(
				id,
				asString(obj, "player_uuid"),
				asString(obj, "slug"),
				asString(obj, "display_name"),
				asString(obj, "status"),
				newTexture,
				asString(obj, "texture_id"),
				recipe,
				files,
				texture
			));
		}
		return out;
	}

	private static String asString(JsonObject obj, String key) {
		if (obj == null || key == null || !obj.has(key) || obj.get(key).isJsonNull()) {
			return null;
		}
		JsonElement el = obj.get(key);
		if (el.isJsonPrimitive()) {
			return el.getAsString();
		}
		return null;
	}

	private static List<String> parseStringArray(String json, String key) {
		List<String> out = new ArrayList<>();
		if (json == null || json.isBlank()) {
			return out;
		}
		try {
			JsonObject root = JsonParser.parseString(json).getAsJsonObject();
			JsonArray arr = root.getAsJsonArray(key);
			if (arr == null) {
				return out;
			}
			for (JsonElement el : arr) {
				if (el != null && el.isJsonPrimitive()) {
					out.add(el.getAsString());
				}
			}
		} catch (Exception ignored) {
			// leave empty
		}
		return out;
	}

	private static SimpleResult getJson(String path) {
		return request("GET", path, null, false);
	}

	private static SimpleResult putJson(String path, String jsonBody) {
		return request("PUT", path, jsonBody, false);
	}

	private static SimpleResult putBytes(String path, byte[] body, String contentType) {
		String base = Cache.apiBaseUrl;
		String key = Cache.pluginKey;
		if (base == null || base.isEmpty() || key == null || key.isEmpty()) {
			return SimpleResult.fail(
				"API is not configured (api.base-url / api.plugin-key in config.yml)."
			);
		}
		if (body == null || body.length == 0) {
			return SimpleResult.fail("Payload is empty.");
		}
		HttpURLConnection connection = null;
		try {
			@SuppressWarnings("deprecation")
			URL url = new URL(base + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("PUT");
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
			connection.setRequestProperty("X-Plugin-Key", key);
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);
			connection.setRequestProperty(
				"Content-Type",
				contentType == null || contentType.isBlank()
					? "application/octet-stream"
					: contentType
			);
			connection.setFixedLengthStreamingMode(body.length);
			try (OutputStream out = connection.getOutputStream()) {
				out.write(body);
			}

			int status = connection.getResponseCode();
			String response = readBody(
				status >= 200 && status < 300
					? connection.getInputStream()
					: connection.getErrorStream()
			);
			if (status >= 200 && status < 300) {
				return SimpleResult.success(response);
			}
			return SimpleResult.fail(detailOrHttp(response, status));
		} catch (Exception e) {
			return SimpleResult.fail("Could not reach API: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static SimpleResult postJson(String path, String jsonBody) {
		return request("POST", path, jsonBody, false);
	}

	private static DownloadResult getBytes(String path) {
		String base = Cache.apiBaseUrl;
		String key = Cache.pluginKey;
		if (base == null || base.isEmpty() || key == null || key.isEmpty()) {
			return DownloadResult.fail(
				"API is not configured (api.base-url / api.plugin-key in config.yml)."
			);
		}
		HttpURLConnection connection = null;
		try {
			@SuppressWarnings("deprecation")
			URL url = new URL(base + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
			connection.setRequestProperty("X-Plugin-Key", key);

			int status = connection.getResponseCode();
			if (status == 200) {
				byte[] data = readBytes(connection.getInputStream());
				if (data == null || data.length == 0) {
					return DownloadResult.fail("Empty file download");
				}
				return DownloadResult.success(data);
			}
			String response = readBody(connection.getErrorStream());
			return DownloadResult.fail(detailOrHttp(response, status));
		} catch (Exception e) {
			return DownloadResult.fail("Could not download: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static SimpleResult request(
		String method,
		String path,
		String jsonBody,
		boolean unused
	) {
		String base = Cache.apiBaseUrl;
		String key = Cache.pluginKey;
		if (base == null || base.isEmpty() || key == null || key.isEmpty()) {
			return SimpleResult.fail(
				"API is not configured (api.base-url / api.plugin-key in config.yml)."
			);
		}
		if (("PUT".equals(method) || "POST".equals(method))
			&& (jsonBody == null || jsonBody.isBlank())) {
			return SimpleResult.fail("Payload is empty.");
		}

		HttpURLConnection connection = null;
		try {
			@SuppressWarnings("deprecation")
			URL url = new URL(base + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			connection.setRequestProperty("X-Plugin-Key", key);
			connection.setRequestProperty("Accept", "application/json");

			if (jsonBody != null) {
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json");
				byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
				connection.setFixedLengthStreamingMode(bytes.length);
				try (OutputStream out = connection.getOutputStream()) {
					out.write(bytes);
				}
			}

			int status = connection.getResponseCode();
			String response = readBody(
				status >= 200 && status < 300
					? connection.getInputStream()
					: connection.getErrorStream()
			);
			if (status >= 200 && status < 300) {
				return SimpleResult.success(response);
			}
			return SimpleResult.fail(detailOrHttp(response, status));
		} catch (Exception e) {
			return SimpleResult.fail("Could not reach API: " + e.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static String detailOrHttp(String response, int status) {
		String detail = jsonString(response, "detail");
		if (detail == null || detail.isEmpty()) {
			detail = response == null || response.isEmpty()
				? ("HTTP " + status)
				: response;
		}
		if (status == 401) {
			return "Unauthorized (check api.plugin-key). " + detail;
		}
		return detail;
	}

	static String escapeJson(String value) {
		if (value == null) {
			return "";
		}
		return value
			.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r");
	}

	static String jsonString(String json, String key) {
		if (json == null || key == null) {
			return null;
		}
		String needle = "\"" + key + "\"";
		int keyIdx = json.indexOf(needle);
		if (keyIdx < 0) {
			return null;
		}
		int colon = json.indexOf(':', keyIdx + needle.length());
		if (colon < 0) {
			return null;
		}
		int i = colon + 1;
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
			i++;
		}
		if (i >= json.length()) {
			return null;
		}
		char c = json.charAt(i);
		if (c == '"') {
			StringBuilder out = new StringBuilder();
			i++;
			while (i < json.length()) {
				char ch = json.charAt(i++);
				if (ch == '\\' && i < json.length()) {
					out.append(json.charAt(i++));
					continue;
				}
				if (ch == '"') {
					break;
				}
				out.append(ch);
			}
			return out.toString();
		}
		if (c == 'n' && json.startsWith("null", i)) {
			return null;
		}
		int start = i;
		while (i < json.length()) {
			char ch = json.charAt(i);
			if (ch == ',' || ch == '}' || ch == ']') {
				break;
			}
			i++;
		}
		return json.substring(start, i).trim();
	}

	private static String readBody(InputStream stream) throws Exception {
		if (stream == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		try (BufferedReader in = new BufferedReader(
			new InputStreamReader(stream, StandardCharsets.UTF_8)
		)) {
			String line;
			while ((line = in.readLine()) != null) {
				sb.append(line);
			}
		}
		return sb.toString();
	}

	private static byte[] readBytes(InputStream stream) throws Exception {
		if (stream == null) {
			return new byte[0];
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = stream.read(buf)) >= 0) {
			out.write(buf, 0, n);
		}
		return out.toByteArray();
	}
}
