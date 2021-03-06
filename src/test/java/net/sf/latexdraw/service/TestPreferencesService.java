package net.sf.latexdraw.service;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import net.sf.latexdraw.util.Page;
import net.sf.latexdraw.util.Unit;
import net.sf.latexdraw.view.GridStyle;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TempDirectory.class)
class TestPreferencesService {
	PreferencesService prefs;

	void assertPreferencesEqual(final PreferencesService p1, final PreferencesService p2) {
		assertEquals(p1.getPathExport(), p2.getPathExport());
		assertEquals(p1.getPathOpen(), p2.getPathOpen());
		assertEquals(p1.gridGapProperty().get(), p2.gridGapProperty().get());
		assertEquals(p1.getRecentFiles(), p2.getRecentFiles());
		assertEquals(p1.gridStyleProperty().get(), p2.gridStyleProperty().get());
		assertEquals(p1.isMagneticGrid(), p2.isMagneticGrid());
		assertEquals(p1.getPage(), p2.getPage());
		assertEquals(p1.checkVersionProperty().get(), p2.checkVersionProperty().get());
		assertEquals(p1.getUnit(), p2.getUnit());
		assertEquals(p1.includesProperty().get(), p2.includesProperty().get());
		assertEquals(p1.getLang(), p2.getLang());
		assertEquals(p1.openGLProperty().get(), p2.openGLProperty().get());
		assertEquals(p1.nbRecentFilesProperty().get(), p2.nbRecentFilesProperty().get());
	}

	@Nested
	class TestEmptyPrefFile {
		@BeforeEach
		void setUp(@TempDirectory.TempDir final Path tempDir) {
			prefs = new PreferencesService(tempDir.toString() + File.separator + "emptyFile.xml");
		}

		@Test
		void getLang() {
			assertEquals(Locale.getDefault(), prefs.getLang());
		}

		@Test
		void getPage() {
			assertEquals(Page.USLETTER, prefs.getPage());
		}

		@Test
		void setPage() {
			prefs.setPage(Page.HORIZONTAL);
			assertEquals(Page.HORIZONTAL, prefs.getPage());
		}

		@Test
		void testIncludesProperty() {
			assertTrue(prefs.includesProperty().get().isEmpty());
		}

		@Test
		void gridGapProperty() {
			assertEquals(10, prefs.gridGapProperty().get());
		}

		@Test
		void isMagneticGrid() {
			assertFalse(prefs.isMagneticGrid());
		}

		@Test
		void getGridStyle() {
			assertEquals(GridStyle.NONE, prefs.getGridStyle());
		}

		@Test
		void getUnit() {
			assertEquals(Unit.CM, prefs.getUnit());
		}

		@Test
		void checkVersionProperty() {
			assertTrue(prefs.checkVersionProperty().get());
		}

		@Test
		void getPathExport() {
			assertTrue(prefs.getPathExport().isEmpty());
		}

		@Test
		void getPathOpen() {
			assertTrue(prefs.getPathOpen().isEmpty());
		}

		@Test
		void getNbRecentFiles() {
			assertEquals(5, prefs.getNbRecentFiles());
		}

		@Test
		void setNbRecentFiles() {
			prefs.setNbRecentFiles(20);
			assertEquals(20, prefs.getNbRecentFiles());
		}

		@Test
		void getCurrentFolder() {
			assertTrue(prefs.getCurrentFolder().isEmpty());
		}

		@Test
		void setCurrentFolder() {
			final File file = new File(System.getProperty("user.home"));
			prefs.setCurrentFolder(file);
			assertEquals(file, prefs.getCurrentFolder().orElseThrow());
		}

		@Test
		void testAddRecentFileWithLimit0() {
			prefs.setNbRecentFiles(0);
			assertTrue(prefs.getRecentFiles().isEmpty());
		}

		@Test
		void testSetLimitNbRecentFiles() {
			prefs.addRecentFile("foo1");
			prefs.addRecentFile("foo2");
			prefs.addRecentFile("foo3");
			prefs.addRecentFile("foo4");
			prefs.addRecentFile("foo5");
			prefs.setNbRecentFiles(3);
			assertEquals(3, prefs.getRecentFiles().size());
			assertEquals("foo3", prefs.getRecentFiles().get(2));
			assertEquals("foo4", prefs.getRecentFiles().get(1));
			assertEquals("foo5", prefs.getRecentFiles().get(0));
		}

		@Test
		void testAddExistingRecentFile() {
			prefs.addRecentFile("foo1");
			prefs.addRecentFile("foo2");
			prefs.addRecentFile("foo3");
			prefs.addRecentFile("foo2");
			assertEquals(3, prefs.getRecentFiles().size());
			assertEquals("foo1", prefs.getRecentFiles().get(2));
			assertEquals("foo3", prefs.getRecentFiles().get(1));
			assertEquals("foo2", prefs.getRecentFiles().get(0));
		}

		@Test
		void testAddRecentFileOnQuota() {
			prefs.setNbRecentFiles(2);
			prefs.addRecentFile("foo1");
			prefs.addRecentFile("foo2");
			prefs.addRecentFile("foo3");
			prefs.addRecentFile("foo4");
			assertEquals(2, prefs.getRecentFiles().size());
			assertEquals("foo3", prefs.getRecentFiles().get(1));
			assertEquals("foo4", prefs.getRecentFiles().get(0));
		}

		@Test
		void testSupportedLocalesNoNull() {
			assertTrue(prefs.getSupportedLocales().stream().noneMatch(l -> l == null));
		}

		@Test
		void getCurrentFile() {
			assertTrue(prefs.getCurrentFile().isEmpty());
		}

		@Test
		void setCurrentFile(@TempDirectory.TempDir final Path tempDir) {
			final File file = Files.newFile(tempDir.toString() + File.separator + "afile.svg");
			prefs.setCurrentFile(file);
			assertEquals(file, prefs.getCurrentFile().orElseThrow());
		}

		@Test
		void getRecentFiles() {
			assertTrue(prefs.getRecentFiles().isEmpty());
		}

		@Test
		void addRecentFile() {
			final String v1 = "foo";
			final String v2 = "bar";
			final String v3 = "third";
			prefs.addRecentFile(v1);
			prefs.addRecentFile(v2);
			prefs.addRecentFile(v3);
			assertEquals(v1, prefs.getRecentFiles().get(2));
			assertEquals(v2, prefs.getRecentFiles().get(1));
			assertEquals(v3, prefs.getRecentFiles().get(0));
		}

		@Test
		void testWritePreferences(@TempDirectory.TempDir final Path tempDir) {
			final File file = Files.newFile(tempDir.toString() + File.separator + "afile.svg");

			prefs.pathOpenProperty().set("pathopen");
			prefs.pathExportProperty().set("exportpath");
			prefs.setCurrentFolder(tempDir.toFile());
			prefs.setCurrentFile(file);
			prefs.gridGapProperty().set(prefs.gridGapProperty().get() * 2);
			prefs.addRecentFile("fooRecent");
			prefs.addRecentFile("barFile");
			prefs.gridStyleProperty().set(GridStyle.CUSTOMISED);
			prefs.magneticGridProperty().set(!prefs.isMagneticGrid());
			prefs.setPage(Page.HORIZONTAL);
			prefs.checkVersionProperty().set(!prefs.checkVersionProperty().get());
			prefs.unitProperty().set(Unit.INCH);
			prefs.includesProperty().set("include text");
			prefs.langProperty().set(prefs.getSupportedLocales().stream().filter(l -> l != prefs.langProperty().get()).findFirst().orElseThrow());
			prefs.openGLProperty().set(!prefs.openGLProperty().get());
			prefs.nbRecentFilesProperty().set(prefs.nbRecentFilesProperty().get() * 3);

			prefs.writePreferences();
			final PreferencesService p2 = new PreferencesService(prefs.getPreferencesPath());
			p2.readPreferences();
			assertPreferencesEqual(prefs, p2);
		}
	}

	@Nested
	class InvalidExistingPrefFile {
		@BeforeEach
		void setUp(@TempDirectory.TempDir final Path tempDir) {
			prefs = new PreferencesService(tempDir.toString());
		}

		@Test
		void testWriteNoCrash() {
			prefs.pathOpenProperty().set("pathopen");
			prefs.pathExportProperty().set("exportpath");
			prefs.writePreferences();
		}

		@Test
		void testReadNoCrash() {
			prefs.readPreferences();
		}

		@Test
		void getLang() {
			assertEquals(Locale.getDefault(), prefs.getLang());
		}
	}
}
