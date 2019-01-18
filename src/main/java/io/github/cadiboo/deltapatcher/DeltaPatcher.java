package io.github.cadiboo.deltapatcher;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Mod;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.minecraftforge.common.ForgeVersion.CheckResult;

/**
 * @author Cadiboo
 */
@Mod(modid = "deltapatcher", version = "0.1.0")
public class DeltaPatcher {

	private static final Logger LOGGER = LogManager.getLogger("deltapatcher");
	static {
		try {
			process("deltapatcher", "0.1.0", "https://raw.githubusercontent.com/Cadiboo/Delta-Patcher/master/patches");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static final int MAX_HTTP_REDIRECTS = 20;

	//find current version of mod
	//find next version, next, next, next

	/**
	 * @param checkResult    the result of a forge version check
	 * @param patchesJSONURI the uri to the patches.json file
	 */
	public static void patch(final CheckResult checkResult, final URI patchesJSONURI) {

//		checkResult.target

	}

	private static void process(final String modId, final String currentModVersion, final String patchesDirectoryURI) throws IOException {
		process(modId, ForgeVersion.mcVersion, currentModVersion, patchesDirectoryURI);
	}

	private static void process(final String modId, final String minecraftVersion, final String currentModVersion, final String patchesDirectoryURI) throws IOException {
		final URL patchesJsonURI = new URL(patchesDirectoryURI + "/" + minecraftVersion + ".json");
		process(modId, currentModVersion, patchesJsonURI);
	}

	private static void process(final String modId, final String currentModVersion, URL patchesJsonURL) throws IOException {

//		try
//		{
//			log.info("[{}] Starting version check at {}", mod.getModId(), url.toString());
//			ForgeVersion.Status status = PENDING;
//			ComparableVersion target = null;

		InputStream con = openUrlStream(patchesJsonURL);
		String data = new String(ByteStreams.toByteArray(con), StandardCharsets.UTF_8);
		con.close();

		LOGGER.debug("[{}] Received version check data:\n{}", modId, data);

		@SuppressWarnings("unchecked")
		Map<String, Object> json = new Gson().fromJson(data, Map.class);

		@SuppressWarnings("unchecked")
		final Map<String, Object> currentVersionPatches = (Map<String, Object>) json.get(currentModVersion);

		final List<String> sortedCurrentVersionPatches = currentVersionPatches.keySet().stream().sorted().collect(Collectors.toList());

		for (String versionPatch : sortedCurrentVersionPatches) {
			final String patchZipUrlString = (String) currentVersionPatches.get(versionPatch);
			final URL patchZipUrl = new URL(patchZipUrlString);
			final String patchName = StringUtils.reverse(StringUtils.reverse(patchZipUrl.getPath()).split("/")[0]);
			final File tempPatchZip = Paths.get("./auto-updater/" + modId + "/" + patchName).toFile();
			downloadZipFile(tempPatchZip, patchZipUrl);
		}

	}

	private static void downloadZipFile(final File outFile, final URL url) {
		try {
			URLConnection conn = url.openConnection();
			InputStream in = conn.getInputStream();
			FileOutputStream out = new FileOutputStream(outFile);
			byte[] b = new byte[1024];
			int count;
			while ((count = in.read(b)) >= 0) {
				out.write(b, 0, count);
			}
			out.flush();
			out.close();
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens stream for given URL while following redirects
	 */
	private static InputStream openUrlStream(URL url) throws IOException {
		if (!url.getProtocol().toLowerCase().equals("https")) {
			throw new SecurityException("protocol must be https");
		}
		URL currentUrl = url;
		for (int redirects = 0; redirects < MAX_HTTP_REDIRECTS; redirects++) {
			URLConnection c = currentUrl.openConnection();
			if (c instanceof HttpURLConnection) {
				HttpURLConnection huc = (HttpURLConnection) c;
				huc.setInstanceFollowRedirects(false);
				int responseCode = huc.getResponseCode();
				if (responseCode >= 300 && responseCode <= 399) {
					try {
						String loc = huc.getHeaderField("Location");
						currentUrl = new URL(currentUrl, loc);
						continue;
					} finally {
						huc.disconnect();
					}
				}
			}
			return c.getInputStream();
		}
		throw new IOException("Too many redirects while trying to fetch " + url);
	}

}
