import java.io.FilenameFilter

class DPIJarFilenameFilter implements FilenameFilter {
	boolean accept(File dir, String name) {
		boolean m = name.indexOf('DPI') > -1 && name.endsWith('.jar');
		// println "$dir, $name: $m" 
		return m
	}
}
