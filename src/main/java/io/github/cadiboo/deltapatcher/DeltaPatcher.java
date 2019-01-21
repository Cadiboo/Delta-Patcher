package io.github.cadiboo.deltapatcher;

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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraftforge.common.ForgeVersion.CheckResult;

/**
 * @author Cadiboo
 */
@Mod(modid = "deltapatcher", version = "0.1.0")
public class DeltaPatcher {

	private static final Logger LOGGER = LogManager.getLogger("deltapatcher");

	public static void main(String... args) throws Exception {

		final File source = Paths.get("/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre7.jar").toFile();
		final File target = Paths.get("/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre6.jar").toFile();
		final File generatedDiff = Paths.get("/Users/Cadiboo/Desktop/diffs/RenderChunk-rebuildChunk-Hooks-1.12.2-0.3.0-pre6to1.12.2-0.3.0-pre7.zip").toFile();

		// locate file system by using the syntax defined in java.net.JarURLConnection
		final URI sourceUri = URI.create("jar:" + source.toURI().toString());
		final URI targetUri = URI.create("jar:" + target.toURI().toString());

		final Map<String, String> env = new HashMap<>();
		env.put("create", "false");

		final FileSystem sourceZipFileSystem = FileSystems.newFileSystem(sourceUri, env);
		final FileSystem targetZipFileSystem = FileSystems.newFileSystem(targetUri, env);

		generatedDiff.getParentFile().mkdirs();
		//why jar not zip? idk, documentation seems to be out of date & using a path in the params doesn't work
		final URI generatedDiffUri = URI.create("jar:" + generatedDiff.toURI().toString());
		final Map<String, String> createEnv = new HashMap<>();
		createEnv.put("create", "true");
		final FileSystem generatedDiffZipFileSystem = FileSystems.newFileSystem(generatedDiffUri, createEnv, null);

		final Path sourceRoot = sourceZipFileSystem.getPath("/");
		final Path targetRoot = targetZipFileSystem.getPath("/");
		final Path generatedDiffRoot = generatedDiffZipFileSystem.getPath("/");

		Files.walk(targetRoot).forEach(file -> {
			final Path pathInSourceJar = sourceRoot.resolve(targetRoot.relativize(file).toString());
			final Path pathInTargetJar = file.toAbsolutePath();
			final Path pathInGeneratedDiffZip = generatedDiffRoot.resolve(targetRoot.relativize(file).toString());
			final Path fullPathInSourceJar = source.toPath().resolve(sourceRoot.relativize(pathInSourceJar).toString());
			final Path fullPathInTargetJar = target.toPath().resolve(targetRoot.relativize(pathInTargetJar).toString());
			final Path fullPathInGeneratedDiffZip = generatedDiff.toPath().resolve(generatedDiffRoot.relativize(pathInGeneratedDiffZip).toString());

			System.out.println(pathInSourceJar);
			System.out.println(pathInTargetJar);
			System.out.println(pathInGeneratedDiffZip);
			System.out.println(fullPathInSourceJar);
			System.out.println(fullPathInTargetJar);
			System.out.println(fullPathInGeneratedDiffZip);

			try {
				if (Files.isDirectory(fullPathInSourceJar)) {
					Files.createDirectories(pathInGeneratedDiffZip);
				} else {
					Files.copy(fullPathInSourceJar, pathInGeneratedDiffZip);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		});

	}

	void s() throws Throwable {
		File zip = null;
		Path contents = null;

		final Map<String, String> env = new HashMap<>();
		//creates a new Zip file rather than attempting to read an existing one
		env.put("create", "true");
		// locate file system by using the syntax
		// defined in java.net.JarURLConnection
		final URI uri = URI.create("jar:file:/" + zip.toString().replace("\\", "/"));
		try (final FileSystem zipFileSystem = FileSystems.newFileSystem(uri, env);
		     final Stream<Path> files = Files.walk(contents)) {
			final Path root = zipFileSystem.getPath("/");
			files.forEach(file -> {
				try {
					copyToZip(root, contents, file);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		}
	}

	/**
	 * Copy a specific file/folder to the zip archive
	 * If the file is a folder, create the folder. Otherwise copy the file
	 *
	 * @param root     the root of the zip archive
	 * @param contents the root of the directory tree being copied, for relativization
	 * @param file     the specific file/folder to copy
	 */
	private static void copyToZip(final Path root, final Path contents, final Path file) throws IOException {
		final Path to = root.resolve(contents.relativize(file).toString());
		if (Files.isDirectory(file)) {
			Files.createDirectories(to);
		} else {
			Files.copy(file, to);
		}
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
