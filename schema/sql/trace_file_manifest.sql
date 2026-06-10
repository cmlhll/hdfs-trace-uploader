CREATE TABLE IF NOT EXISTS trace_file_manifest (
  file_id              STRING,
  app                  STRING,
  env                  STRING,
  region               STRING,
  cluster              STRING,
  host                 STRING,
  pid                  STRING,
  boot_id              STRING,
  dt                   STRING,
  hour                 INT,
  bucket               INT,
  start_time           TIMESTAMP,
  end_time             TIMESTAMP,
  hdfs_path            STRING,
  size_bytes           BIGINT,
  checksum             STRING,
  record_count         BIGINT,
  raw_format           STRING,
  compression          STRING,
  upload_attempt       INT,
  upload_start_time    TIMESTAMP,
  upload_finish_time   TIMESTAMP,
  commit_time          TIMESTAMP,
  agent_id             STRING,
  agent_version        STRING,
  state                STRING,
  error_message        STRING
)
PARTITIONED BY (
  manifest_dt STRING,
  manifest_hour INT
)
STORED AS ORC;
