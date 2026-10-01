package com.uap.proiv.jobs.service;

import com.uap.proiv.jobs.dto.Job;
import com.uap.proiv.jobs.dto.JobRequest;

import java.util.List;

public interface JobService {
    List<Job> getAllJobs();
    Job getJobById(int id);
    Job add(JobRequest request);
}
