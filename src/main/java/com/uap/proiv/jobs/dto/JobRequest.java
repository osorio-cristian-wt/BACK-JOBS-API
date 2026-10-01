package com.uap.proiv.jobs.dto;

public class JobRequest {
    private String name;
    private double salary;
    private int hours;
    private int resources;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSalary() {
        return salary;
    }

    public void setSalary(double salary) {
        this.salary = salary;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getResources() {
        return resources;
    }

    public void setResources(int resources) {
        this.resources = resources;
    }

    @Override
    public String toString() {
        return "Job {" +
                "name='" + name + '\'' +
                ", salary=" + salary +
                ", hours=" + hours +
                ", resources=" + resources +
                '}';
    }
}
