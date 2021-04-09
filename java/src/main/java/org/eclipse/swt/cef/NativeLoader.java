/**
 * Turn on absolut path to jnilib and OpenCascade libraries!
 * 
 * "-Dde.ict.nativeloader.debug=true" to load from %temp%/occ_java_libs
 * 
 * Names of required libs are stored in requiredLibs-Array!
 * 
 * *** BE CAREFUL!!! ***
 * Java needs to load libraries in a very well defined order for them to work as
 * they depend on each other!
 * Currently this only seems to affect windows.
 * 
 * Windows: Use Dependencies (https://github.com/lucasg/Dependencies) to check
 * import order!
 * macOS: otool -L <lib>
 * linux: ldd <lib>
 * 
 */

package org.eclipse.swt.cef;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NativeLoader {
	private static AtomicBoolean isLoaded = new AtomicBoolean();
	private static String OS = System.getProperty("os.name").toLowerCase();
	
	public static enum ResultNativeLoad {
		SUCCESS(1), ERROR(-1), ALREADY_LOADED(2);

		private final int value;

		ResultNativeLoad(int value) {
			this.value = value;
		}

		public int get() {
			return value;
		}

		public boolean successLoad() {
			return (value == 1);
		}

		public boolean alreadyLoaded() {
			return (value == 2);
		}

		public boolean failedLoad() {
			return (value == -1);
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(NativeLoader.class);

	private static boolean nativeOsgiResolver = false;

	private static String[] linuxLibs = new String[] {
			// @formatter:off
			// 3rd-parties
			// "tcl86",
			// "tk86",
			// @formatter on
	};

	private static String[] windowsLibs = new String[] {
			// @formatter:off
			// 3rd-parties
			// "tcl86",
			// "tk86",
			// @formatter on
	};

	private static String[] macosLibs = new String[] {
			// @formatter:off
			// 3rd-parties
			// "tcl86",
			// "tk86",
			//"FreeImage",
			//"avutil-55",
			//"swresample",
			//"avcodec-57",
			//"avformat-57",
			//"swscale-4"
			// @formatter on
	};

	private static String[] chromiumLibs = new String[] {
			// @formatter:off
			// cef foundation
			"chrome_elf",
			"libcef",

			// At last our library
			"swt-cef"
			// @formatter:on
	};

	/**
	 * Set the OSGI native libraries resolver (default = false). </br>
	 * In a standard java environment, the native libraries are unpacked into a
	 * temporary directory and then loaded. </br>
	 * In an Eclipse environment, the native library can be managed by the
	 * Eclipse platform.
	 * 
	 * @param nativeOsgiResolver
	 *            set to <code>true</code> on Eclipse plattform
	 * 
	 */
	public static void setNativeOsgiResolver(boolean nativeOsgiResolver) {
		NativeLoader.nativeOsgiResolver = nativeOsgiResolver;
	}

	/**
	 * Default resolver for native libraries
	 */
	private static BiFunction<String, String[], ResultNativeLoad> nativeLibraryDefaultResolver = (path, requiredLibs) -> {
		try {
			final ProtectionDomain pd = NativeLoader.class.getProtectionDomain();
			final CodeSource cs = pd.getCodeSource();
			final URL location = cs.getLocation();

			Path nativeLibraryPath = null;

			// Extract from JAR-File
			// location liefert ein Pfad zu JAR-Datei wenn JAR verwendet wird...
			if (location.getPath().endsWith(".jar")) {
				// Temporary library folder
				final Path tempFolder = new File(System.getProperty("java.io.tmpdir")).toPath().resolve("occ_java_libs");

				if (!Boolean.valueOf(System.getProperty("org.eclipse.swt.cef.nativeloader.debug", "false"))) {
					if (!Files.exists(tempFolder)) {
						try {
							Files.createDirectories(tempFolder);
						} catch (final IOException e) {
							LOGGER.error("create temp folder: " + e.getMessage());
						}
					} else {
						cleanTempDirectory(tempFolder.toFile());
					}

					// Extract resource files
					LOGGER.info("Extracting Opencascade to " + tempFolder.toAbsolutePath().toString());
					if (extractLibraryFiles(location.toURI(), path, tempFolder)) {
						nativeLibraryPath = tempFolder;
					}
				} else {
					nativeLibraryPath = tempFolder;
				}
			} else {
				// Return folder path on IDE debug
				// ... wenn die Ressourcen schon entpackt sind (z.B. beim debugen in der IDE) 
				// wird der Pfad zusammengebaut und zurückgegeben.  
				nativeLibraryPath = Paths.get(location.toURI()).resolve(path);
			}

			if (nativeLibraryPath.toFile().isDirectory()) {
				LOGGER.info("Loading OpenCascade from " + nativeLibraryPath.toString());
				for (final String lib : collectLibraries(nativeLibraryPath, requiredLibs)) {
					System.load(lib);
				}
			} else {
				throw new Exception("Native library path must be a directory!");
			}
			return ResultNativeLoad.SUCCESS;
		} catch (final Exception e) {
			LOGGER.error("Native library loader exception:", e);
			throw new RuntimeException(e);
		}
	};

	/**
	 * Eclipse\OSGI resolver for native libraries
	 */
	private static BiFunction<String, String[], ResultNativeLoad> nativeLibraryOsgiResolver = (path, requiredLibs) -> {
		try {
			for (final String libname : requiredLibs) {
				System.loadLibrary(libname);
			}
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
		return ResultNativeLoad.SUCCESS;
	};

	private static String[] convertLibraryNameByPlatform(String[] names) {
		final String[] filenames = new String[names.length];
		if (isWindows()) {
			for (int i = 0; i < names.length; i++) {
				filenames[i] = names[i] + ".dll";
			}
		} else if (isLinux()) {
			for (int i = 0; i < names.length; i++) {
				filenames[i] = "lib" + names[i] + ".so";
			}
		} else if (isMac()) {
			for (int i = 0; i < names.length; i++) {
				if ("occjava".equals(names[i])) {
					filenames[i] = "lib" + names[i] + ".jnilib";
				} else {
					filenames[i] = "lib" + names[i] + ".dylib";
				}
			}
		}
		return filenames;
	}

	private static Path checkSymLinks(Path file) {
		if (isWindows()) {
			return file;
		}

		if (Files.isSymbolicLink(file)) {
			try {
				final Path origin = Files.readSymbolicLink(file);
				final Path path = Paths.get(file.getParent().toString(), origin.toString());
				return checkSymLinks(path);
			} catch (final IOException e) {
				LOGGER.error("checkSymLinks: " + e.getMessage());
			}
		} else {
			if (file.toFile().exists()) {
				return file;
			} else {
				final File[] files = file.toFile().getParentFile().listFiles((FilenameFilter) (dir, name) -> {
					String fileName = file.getFileName().toString();
					fileName = fileName.substring(0, fileName.indexOf("."));
					return name.startsWith(fileName);
				});
				if (files.length == 0) {
					return Paths.get(".", "invalid");
				}
				return checkSymLinks(files[0].toPath());
			}
		}
		return Paths.get(".", "invalid");
	}

	private static String getVersionVariant(URI uri, String file) throws Exception {
		final List<String> retList = new ArrayList<>();
		final String prefix = file.substring(0, file.indexOf(".") + 1);
		try {
			final Path libPath = Paths.get(uri);
			Files.list(libPath).forEach(path -> {
				if (path.getFileName().toString().startsWith(prefix)) {
					retList.add(path.getFileName().toString());
				}
			});
		} catch (final Exception e) {
			e.printStackTrace();
			LOGGER.error("Error in getVersionVariants: " + e.getMessage());
		}
		if (retList.isEmpty()) {
			LOGGER.error("Can't find any required variant of " + file + "!");
			throw new FileNotFoundException("Can't find any required variant of " + file + "!");
		}
		return retList.get(0);
	}

	private static String[] joinArrays(String[] arr1, String[] arr2) {
		final String[] ret = new String[arr1.length + arr2.length];
		int i = 0;
		for (int j = 0; j < arr1.length; j++) {
			ret[i++] = arr1[j];
		}
		for (int j = 0; j < arr2.length; j++) {
			ret[i++] = arr2[j];
		}
		return ret;
	}

	private static void cleanTempDirectory(File dir) {
		for (final File file : dir.listFiles()) {
			if (file.isDirectory()) {
				cleanTempDirectory(file);
			}
			file.delete();
		}
	}

	private static List<String> collectLibraries(Path path, String[] requiredLibs) throws Exception {
		final String[] filenames = convertLibraryNameByPlatform(requiredLibs);
		final List<String> libList = new ArrayList<>();
		for (final String file : filenames) {
			final String variant = getVersionVariant(path.toUri(), file);
			final File lib = checkSymLinks(Paths.get(path.toString(), variant)).toFile();
			if (lib.exists()) {
				try {
					libList.add(lib.getAbsolutePath());
				} catch (final Exception e) {
					e.printStackTrace();
				}
			} else {
				LOGGER.error("Missing dependency " + file);
			}
		}
		return libList;
	}

	private static boolean extractLibraryFiles(URI uri, String path, Path targetFolder) throws URISyntaxException {
		final String jarFileName = uri.getPath();
		try (final FileInputStream fis = new FileInputStream(new File(jarFileName))) {
			final ZipInputStream zis = new ZipInputStream(fis);
			ZipEntry ze = zis.getNextEntry();
			while (ze != null) {
				if (ze.isDirectory()) {
					ze = zis.getNextEntry();
					continue;
				}

				if (ze.getName().startsWith(path)) {
					final File tmp = new File(ze.getName());
					final File extractedLibFile = new File(targetFolder.toString(), tmp.getName());
					final BufferedInputStream in = new BufferedInputStream(zis);
					try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(extractedLibFile))) {
						final byte buffer[] = new byte[4096];
						int len;
						while ((len = in.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}
						if (!isWindows()) {
							Runtime.getRuntime().exec(new String[] { "chmod", "755", extractedLibFile.getAbsolutePath() }).waitFor();
						}
					}
				}
				ze = zis.getNextEntry();
			}
		} catch (final Exception e) {
			LOGGER.error(e.getMessage());
			return false;
		}
		return true;
	}

	public static boolean isWindows() {
		return (OS.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		return (OS.indexOf("mac") >= 0);
	}

	public static boolean isLinux() {
		return (OS.indexOf("nux") >= 0);
	}

	public static ResultNativeLoad load() {
		if (isLoaded.compareAndSet(false, true)) {
			// Load the os-dependent library from the jar file
			String dbrNativeLibraryName = System.mapLibraryName("swt-cef");
			if (dbrNativeLibraryName != null && dbrNativeLibraryName.endsWith("dylib")) {
				dbrNativeLibraryName = dbrNativeLibraryName.replace("dylib", "jnilib");
			}

			String[] requiredLibs = new String[] {};
			String palatformPath = null;
			if (isWindows()) {
				palatformPath = "os/win32/x86_64";
				requiredLibs = joinArrays(windowsLibs, chromiumLibs);
			} else if (isLinux()) {
				palatformPath = "os/linux/x86_64";
				requiredLibs = joinArrays(linuxLibs, chromiumLibs);
			} else if (isMac()) {
				palatformPath = "os/macosx/x86_64";
				requiredLibs = joinArrays(macosLibs, chromiumLibs);
			}

			if (nativeOsgiResolver) {

				return nativeLibraryOsgiResolver.apply(palatformPath, requiredLibs);
			} else {
				return nativeLibraryDefaultResolver.apply(palatformPath, requiredLibs);
			}
		} else {
			return ResultNativeLoad.ALREADY_LOADED;
		}

	}
}
