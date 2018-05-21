package uk.co.cacoethes.lazybones.commands

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.S3ObjectInputStream
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.codehaus.groovy.runtime.IOGroovyMethods
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import uk.co.cacoethes.lazybones.PackageNotFoundException
import uk.co.cacoethes.util.S3Utils

/**
 * Handles the retrieval of template packages from Bintray (or other supported
 * repositories).
 */
@CompileStatic
@Log
class PackageDownloader {

    File downloadPackage(PackageLocation packageLocation, String packageName, String version) {
        def packageFile = new File(packageLocation.cacheLocation)

        if (!packageFile.exists()) {
            packageFile.parentFile.mkdirs()

            // The package info may not have been requested yet. It depends on
            // whether the user specified a specific version or not. Hence we
            // try to fetch the package info first and only throw an exception
            // if it's still null.
            //
            // There is an argument for having getPackageInfo() throw the exception
            // itself. May still do that.
            log.fine "${packageLocation.cacheLocation} is not cached locally. Searching the repositories for it."
            log.fine "Attempting to download ${packageLocation.remoteLocation} into ${packageLocation.cacheLocation}"

            URL locationUrl = new URL(packageLocation.remoteLocation)

            try {
                packageFile.withOutputStream { OutputStream out ->
                    InputStream stream
                    if (S3Utils.isS3(packageName)) {
                        stream = getS3Stream(locationUrl)
                    } else {
                        stream = getHttpStream(locationUrl)
                    }
                    IOGroovyMethods.withStream(stream) { InputStream input ->
                        out << input
                    }
                }
            }
            catch (FileNotFoundException ex) {
                packageFile.deleteOnExit()
                throw new PackageNotFoundException(packageName, version, ex)
            }
            catch (all) {
                packageFile.deleteOnExit()
                throw all
            }
        }

        return packageFile

    }

    private BufferedInputStream getHttpStream(URL locationUrl) {
        return ResourceGroovyMethods.newInputStream(locationUrl)
    }

    private S3ObjectInputStream getS3Stream(URL locationUrl) {
        AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient()
        String bucket = locationUrl.host.substring(0, locationUrl.host.indexOf("."))
        String key = locationUrl.path.substring(1)
        return s3.getObject(bucket, key).objectContent
    }
}
