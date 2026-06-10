package com.company.traceuploader.state;
import com.company.traceuploader.model.TraceFileMetadata;
public class UploadFileRecordFactory {
    public static UploadFileRecord fromMetadata(TraceFileMetadata m, UploadState state, int attempt) { UploadFileRecord r=new UploadFileRecord(); r.fileId=m.fileId(); r.app=m.app(); r.env=m.env(); r.region=m.region(); r.cluster=m.cluster(); r.host=m.host(); r.pid=m.pid(); r.bootId=m.bootId(); r.localPath=m.localPath().toString(); r.donePath=m.donePath().toString(); r.hdfsStagingPath=m.hdfsStagingPath(); r.hdfsFinalPath=m.hdfsFinalPath(); r.dt=m.dt().toString(); r.hour=m.hour(); r.bucket=m.bucket(); r.startTimeMs=m.startTime().toEpochMilli(); r.endTimeMs=m.endTime().toEpochMilli(); r.sizeBytes=m.sizeBytes(); r.checksum=m.checksum(); r.recordCount=m.recordCount(); r.state=state; r.attempt=attempt; return r; }
}
