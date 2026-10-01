package com.uap.proiv.jobs.service.impl;

import com.uap.proiv.jobs.dto.*;
import com.uap.proiv.jobs.service.AssignedService;
import com.uap.proiv.jobs.service.JobService;
import com.uap.proiv.jobs.service.UserJobAssignedService;
import com.uap.proiv.jobs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserJobAssignedServiceImpl implements UserJobAssignedService {
    private final JobService jobService;
    private final UserService userService;
    private final AssignedService assignedService;
    @Autowired
    public UserJobAssignedServiceImpl(JobService jobService, UserService userService, AssignedService assignedService) {
        this.jobService = jobService;
        this.userService = userService;
        this.assignedService = assignedService;
    }
    @Override
    public List<UserJobAssigned> assign() {
        List<Job> jobs = jobService.getAllJobs();
        UserApiResponse userApiResponse = userService.search(1);
        List<User> users = new ArrayList<>(userApiResponse.getData());
        int totalPages = userApiResponse.getTotalPages();
        while (userApiResponse != null && userApiResponse.getPage() <= totalPages){
            userApiResponse = userService.search(userApiResponse.getPage() + 1);
            if (userApiResponse != null && userApiResponse.getData() != null) {
                users.addAll(userApiResponse.getData());
            }
        }
        List<AssignedResponse> assigned = assignedService.create(jobs, users.stream().map(User::getId).collect(Collectors.toList()));

        return jobs.stream().map(job -> {
           List<User> usersJob = assigned.stream().filter(a -> a.jobId() == job.getId())
                   .map(assignedResponse ->
                           users.stream().filter(u -> u.getId() == assignedResponse.userId()).findFirst().orElseThrow()
                   ).toList();
           return new UserJobAssigned(usersJob, job);
        }).toList();

    }
}
