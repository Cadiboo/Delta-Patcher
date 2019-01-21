package io.github.cadiboo.deltapatcher;

import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import net.minecraftforge.common.ForgeVersion;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.repackage.com.nothome.delta.Delta;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardOpenOption.READ;
import static net.minecraftforge.common.ForgeVersion.CheckResult;

/**
 * @author Cadiboo
 */
@Mod(modid = "deltapatcher", version = "0.1.0")
public class DeltaPatcher {

	private static final Logger LOGGER = LogManager.getLogger("deltapatcher");
//	static {
//		try {
//			process("deltapatcher", "0.1.0", "https://raw.githubusercontent.com/Cadiboo/Delta-Patcher/master/patches");
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}

	public static void main(String... args) throws Exception {
//		Delta.main(args);

		final ZipFile source = new ZipFile("/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre7.jar");
		final ZipFile target = new ZipFile("/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre6.jar");
		final File generatedDiff = Paths.get("/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre6to1.12.2-0.3.0-pre7.zip").toFile();

		final ZipInputStream sourceZipInputStream = new ZipInputStream(Files.newInputStream(source., READ));
		final ZipInputStream targetZipInputStream = new ZipInputStream(Files.newInputStream(target.toPath(), READ));
		generatedDiff.getParentFile().mkdirs();
		final ZipInputStream generatedDiffZipInputStream = new ZipInputStream(Files.newInputStream(generatedDiff.toPath(), READ));

		final HashSet<String> targetFileNames = Sets.newHashSet();
		final HashSet<ZipEntry> targetZipEntries = Sets.newHashSet();
		{
			ZipEntry zipEntry;
			while ((zipEntry = targetZipInputStream.getNextEntry()) != null) {
				if (zipEntry.isDirectory()) {
					continue;
				}
				targetFileNames.add(zipEntry.getName());
				targetZipEntries.add((ZipEntry) zipEntry.clone());
			}
		}

		ZipEntry zipEntry;
		while ((zipEntry = sourceZipInputStream.getNextEntry()) != null) {
			if (zipEntry.isDirectory()) {
				continue;
			}
			System.out.println(zipEntry);
			System.out.println(targetFileNames.contains(zipEntry.getName()));

			args = new String[]{
					source.getI,
					Paths.get(target.getName(), zipEntry.getName()).toString(),
					Paths.get(generatedDiff.getName(), zipEntry.getName()).toString(),
			};

			Delta.main(args);

		}

		sourceZipInputStream.close();
		targetZipInputStream.close();

		System.exit(0);

		args = new String[]{
				"/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre7.jar",
				"/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre6.jar",
				"/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre6to1.12.2-0.3.0-pre7.zip",
		};

		Delta.main(args);
	}

	private static final int MAX_HTTP_REDIRECTS = 20;

	public static void genPatch(File source, File target) {
		String[] args = {
				"-d",
				source.getAbsolutePath(),
				target.getAbsolutePath()
		};
		try {
			Delta.main(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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

		@SuppressWarnings("unchecked") final Map<String, Object> currentVersionPatches = (Map<String, Object>) json.get(currentModVersion);

		final List<String> sortedCurrentVersionPatches = currentVersionPatches.keySet().stream().sorted().collect(Collectors.toList());

		for (String versionPatch : sortedCurrentVersionPatches) {
			final String patchZipUrlString = (String) currentVersionPatches.get(versionPatch);
			final URL patchZipUrl = new URL(patchZipUrlString);
			final String patchName = StringUtils.reverse(StringUtils.reverse(patchZipUrl.getPath()).split("/")[0]);
			final File tempPatchZip = Paths.get("./auto-updater/" + modId + "/" + patchName).toFile();
			try {
				downloadZipFile(tempPatchZip, patchZipUrl);
			} catch (IOException e) {
				//move to next on error
				continue;
			}
			//copy mod file to temp dir and patch
			break;
		}

	}

	private static void downloadZipFile(final File outFile, final URL url) throws IOException {
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
