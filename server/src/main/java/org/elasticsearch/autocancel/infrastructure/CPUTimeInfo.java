package org.elasticsearch.autocancel.infrastructure;

public class CPUTimeInfo {
    private Integer previousVersion;

    private Integer version;

    private Long previousCPUTime;

    private Long cpuTime;

    public CPUTimeInfo() {
        this.previousVersion = 0;
        this.version = 0;
        this.previousCPUTime = 0L;
        this.cpuTime = 0L;
    }

    public CPUTimeInfo(Integer version, Long cpuTime) {
        assert version > 0 && cpuTime > 0L : "version and cpu time should be a positive value.";

        this.previousVersion = 0;
        this.version = version;
        this.previousCPUTime = 0L;
        this.cpuTime = cpuTime;
    }

    public void update(Integer version, Long cpuTime) {
        assert version >= this.version && cpuTime >= this.cpuTime : "version and cpu time should never decrease.";

        this.previousVersion = this.version;
        this.previousCPUTime = this.cpuTime;

        this.version = version;
        this.cpuTime = cpuTime;
    }

    public Integer getPreviousVersion() {
        return this.previousVersion;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Long diffCPUTime() {
        return this.cpuTime - this.previousCPUTime;
    }

    public Boolean comparable(CPUTimeInfo cpuTimeInfo) {
        return this.previousVersion == cpuTimeInfo.getPreviousVersion() && this.version == cpuTimeInfo.getVersion();
    }
}
