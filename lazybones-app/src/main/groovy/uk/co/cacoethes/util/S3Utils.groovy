package uk.co.cacoethes.util

import groovy.transform.CompileStatic

/**
 * Created by Stephen Bellchester on 21/05/2018.
 */
@CompileStatic
class S3Utils {

    /**
     * Determines whether the given package name is in fact an S3 URL.
     */
    static boolean isS3(String str) {
        if (!str) return false
        return str.startsWith("s3://")
    }
}
