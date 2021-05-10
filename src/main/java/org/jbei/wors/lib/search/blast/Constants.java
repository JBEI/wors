package org.jbei.wors.lib.search.blast;

import java.io.File;

// todo : move to properties file / database
public class Constants {

    // location of data directory
    public static final String DATA_DIR = "/var/lib/wors";

    public static final String TMP_DIR = "/tmp";

    public static final String GENBANK_INPUT_DIR = DATA_DIR + File.separator + "input";

    // blast installation location (must point to the "bin" folder)
    public static final String BLAST_INSTALL = "/usr/bin";

    public static final String PUBLIC_REGISTRY_TOKEN = "uYnOZjXiPO9kcs2M6ncHqfWPJ3UhPLVI0j9SvQLQiuQ=";

    public static final String PUBLIC_REGISTRY_TOKEN_CLIENT = "webofregistries.org";

    public static final String PUBLIC_REGISTRY_TOKEN_OWNER = "haplahar@lbl.gov";

    // blast folder name within the data directory
    public static final String BLAST_DB_FOLDER = DATA_DIR + File.separator + "blast";

    public static final String LUCENE_INDEX_FOLDER = DATA_DIR + File.separator + "index";

    // name of the blast database
    public static final String BLAST_DB_NAME = "wors";

    // lock file for writing fasta sequences for blast database
    public static final String LOCK_FILE_NAME = "sequence_write.lock";

    // name to use when a new blast index file is to be used
    public static final String NEW_INDEX_FILE_NAME = "bigfastafile.new";

    public static final String BLAST_DB_FOLDER_NAME = "blast";

    public static final String ADDGENE_URL_PREFIX = "https://www.addgene.org";

    // url prefix for retrieving the part information from IGEM in xml format
    public static final String IGEM_XML_PART_URL_PREFIX = "http://parts.igem.org/cgi/xml/part.cgi?part=";

    public static final String IGEM_PART_URL_PREFIX = "http://parts.igem.org";

    public static final String IGEM_ALL_PARTS_URL = "http://parts.igem.org/fasta/parts/All_Parts";

    public static final String NCBI_SEARCH_URL = "https://www.ncbi.nlm.nih.gov/search/api/sequence"; ///AC163214/?report=fasta

    // limit on genbank sequence size (in bps). set to empty for unlimited
    public static final String GENBANK_BP_LIMIT = "1000000";

    public static final String MASTER_REGISTRY_URL = "public-registry.jbei.org";

    public static final String MASTER_REGISTRY_NAME = "Public Registry";

    public static final String MAX_PARTS_PER_REGISTRY = "0";
}
