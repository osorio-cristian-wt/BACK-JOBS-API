package com.uap.proiv.jobs.service.impl;

import com.uap.proiv.jobs.client.JobApiRepository;
import com.uap.proiv.jobs.dto.Job;
import com.uap.proiv.jobs.dto.JobRequest;
import com.uap.proiv.jobs.service.JobService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class JobServiceImpl implements JobService {
    private final JobApiRepository jobApiRepository;

    @Autowired
    public JobServiceImpl(JobApiRepository jobApiRepository) {
        this.jobApiRepository = jobApiRepository;
    }

    @Override
    public List<Job> getAllJobs() {
        return jobApiRepository.getAllJobs();
    }

    @Override
    public Job getJobById(int id) {
        List<Job> jobs = jobApiRepository.getAllJobs();
        return jobs.stream().filter(job -> job.getId() == id).findFirst().orElseThrow();
    }

    @Override
    public Job add(JobRequest request) {
        return new Job(
                request.getName(),
                request.getSalary(),
                request.getHours(),
                99,
                request.getResources()
        );
    }
}
