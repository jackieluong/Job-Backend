package com.example.Job.controller;

import com.example.Job.constant.IndustryEnum;
import com.example.Job.constant.JobStatusEnum;
import com.example.Job.constant.JobTypeEnum;
import com.example.Job.constant.LevelEnum;
import com.example.Job.models.ResultPagination;
import com.example.Job.models.dtos.*;
import com.example.Job.service.IJobSaveService;
import com.example.Job.service.IJobService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final IJobService jobService;
    private final IJobSaveService jobSaveService;
    public JobController(IJobService jobService, IJobSaveService jobSaveService) {
        this.jobService = jobService;
        this.jobSaveService = jobSaveService;
    }

    @PostMapping("/save")
    public ResponseEntity<ResponseDto> saveJob(@RequestParam(value = "jobId") Long jobId) {


        jobSaveService.saveJob(jobId);

        ResponseDto response = ResponseDto.builder()
                .data(null)
                .message("Save Job successfully")
                .status(HttpStatus.OK)
                .isSuccess(true)
                .build();


        return new ResponseEntity(response, HttpStatus.OK);

    }

    // This Get Job Detail API is for User
    @GetMapping("/detail/{id}")
    public ResponseEntity<ResponseDto> getJobDetailForUser(@PathVariable(name = "id") long id) {


        JobDetailCompanyResponse jobDetail = jobService.getJobDetailWithCompanyById(id);

        ResponseDto responseDto = new ResponseDto.Builder()
                .setStatus(HttpStatus.OK)
                .setMessage("Get Job Detail successfully")
                .setData(jobDetail)
                .build();
        return new ResponseEntity(responseDto, HttpStatus.OK);

    }

    // This Get Job Detail API is for Recruiter
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto> getJobDetail(@PathVariable(name = "id") long id) {


        JobDetailResponse jobDetail = jobService.getJobDetailById(id);

        ResponseDto responseDto = new ResponseDto.Builder()
                .setStatus(HttpStatus.OK)
                .setMessage("Get Job Detail successfully")
                .setData(jobDetail)
                .build();
        return new ResponseEntity(responseDto, HttpStatus.OK);

    }

    @PostMapping
    public ResponseEntity<ResponseDto> createJob(@Valid @RequestBody JobCreateRequest jobCreateRequest) {


        JobDetailResponse addJob = jobService.createJob(jobCreateRequest);

        ResponseDto responseDto = new ResponseDto.Builder()
                .setStatus(HttpStatus.CREATED)
                .setMessage("Create new Job successfully")
                .setData(addJob)
                .build();
        return new ResponseEntity(responseDto, HttpStatus.CREATED);

    }

    @GetMapping
    public ResponseEntity<ResultPagination> getJobsPagination(@RequestParam(value = "currentPage", defaultValue = "1") int current,
                                                                      @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                                      @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
                                                                      @RequestParam(value = "ascending", defaultValue = "false") String isAscending) {

//        int defaultPage = 1;
//        int defaultPageSize = 5;
//
//        int currentPage =current.isPresent() ? Integer.parseInt(current.get()) : defaultPage;
//        int size = pageSize.isPresent() ? Integer.parseInt( pageSize.get()) : defaultPageSize;

//        List<JobDto> jobDtos = jobService.getAllJobs(current - 1, pageSize);

        Page<GetJobResponse> jobResPage = jobService.getAllJobs(current - 1, pageSize, sortBy, Boolean.valueOf(isAscending));

        ResultPagination<GetJobResponse> res = ResultPagination.<GetJobResponse>builder()
                .isSuccess(true)
                .message("Get jobs successfully")
                .httpStatus(HttpStatus.OK)
                .currentPage(jobResPage.getNumber() + 1)
                .pageSize(jobResPage.getSize())
                .previousPage(jobResPage.hasPrevious() ? jobResPage.getNumber() : null)
                .nextPage(jobResPage.hasNext() ? jobResPage.getNumber() + 2 : null)
                .data(jobResPage.getContent())
                .totalPage(jobResPage.getTotalPages())
                .totalElement(jobResPage.getTotalElements())
                .build();


        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/company")
    public ResponseEntity<ResultPagination> getJobByCompany(@RequestParam(value = "companyId") Long companyId,
                                                                    @RequestParam(value = "currentPage", defaultValue = "1") int current,
                                                                    @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                                    @RequestParam(value = "sortBy", defaultValue = "yearOfExperience") String sortBy,
                                                                    @RequestParam(value = "ascending", defaultValue = "true") String isAscending) {


            Page<GetJobResponse> jobResPage = jobService.getAllJobsByCompany(companyId, current - 1, pageSize, sortBy, Boolean.valueOf(isAscending));

            ResultPagination<GetJobResponse> res = ResultPagination.<GetJobResponse>builder()
                    .isSuccess(true)
                    .message("Get jobs successfully")
                    .httpStatus(HttpStatus.OK)
                    .currentPage(jobResPage.getNumber() + 1)
                    .pageSize(jobResPage.getSize())
                    .previousPage(jobResPage.hasPrevious() ? jobResPage.getNumber() : null)
                    .nextPage(jobResPage.hasNext() ? jobResPage.getNumber() + 2 : null )
                    .data(jobResPage.getContent())
                    .totalPage(jobResPage.getTotalPages())
                    .totalElement(jobResPage.getTotalElements())
                    .build();


            return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/company/post")
    public ResponseEntity<ResultPagination> getJobPostByCompany(@RequestParam(value = "companyId") Long companyId,
                                                            @RequestParam(value = "currentPage", defaultValue = "1") int current,
                                                            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
                                                            @RequestParam(value = "ascending", defaultValue = "true") String isAscending,
                                                                @RequestParam(value = "postStatus", required = false) JobStatusEnum postStatus
    ) {

//        JobStatusEnum jobStatusEnum =  postStatus != null ? JobStatusEnum.valueOf(postStatus) : null;

        Page<JobPostResponse> jobResPage = jobService.getAllJobPostsByCompany(companyId,postStatus ,current - 1, pageSize, sortBy, Boolean.valueOf(isAscending));

        ResultPagination<JobPostResponse> res = ResultPagination.<JobPostResponse>builder()
                .isSuccess(true)
                .message("Get job posts successfully")
                .httpStatus(HttpStatus.OK)
                .currentPage(jobResPage.getNumber() + 1)
                .pageSize(jobResPage.getSize())
                .previousPage(jobResPage.hasPrevious() ? jobResPage.getNumber() : null)
                .nextPage(jobResPage.hasNext() ? jobResPage.getNumber() + 2 : null )
                .data(jobResPage.getContent())
                .totalPage(jobResPage.getTotalPages())
                .totalElement(jobResPage.getTotalElements())
                .build();


        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<ResultPagination> searchForJobs(@RequestParam(value = "currentPage", defaultValue = "1") int current,
                                                              @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                              @RequestParam(value = "sortBy", required = false) String sortBy,
                                                              @RequestParam(value = "ascending", required = false) String isAscending,
                                                          @RequestParam(value = "keyword", required = false) String keyword,
                                                          @RequestParam(value = "jobType", required = false) JobTypeEnum jobType,
                                                          @RequestParam(value = "industry", required = false) IndustryEnum industry,
                                                          @RequestParam(value = "level", required = false) LevelEnum level,
                                                          @RequestParam(value = "minExperience", required = false) Integer minExperience,
                                                          @RequestParam(value = "maxExperience", required = false) Integer maxExperience,
                                                          @RequestParam(value = "minSalary", required = false) Double minSalary,
                                                          @RequestParam(value = "maxSalary", required = false) Double maxSalary,
                                                          @RequestParam(value = "cities", required = false) List<String> cities
                                                          ) {

//        double minSal = minSalary == null ? 0 : minSalary;
//        double maxSal = maxSalary == null ? Double.MAX_VALUE : maxSalary;

        JobFilter jobFilter = JobFilter.builder()
                .keyword(keyword)
                .jobType(jobType)
                .industry(industry)
                .level(level)
                .minExperience(minExperience)
                .maxExperience(maxExperience)
                .minSalary(minSalary)
                .maxSalary(maxSalary)
                .cities(cities)
                .build();
        Page<GetJobResponse> jobResPage = jobService.searchForJobs(current - 1, pageSize, sortBy, Boolean.valueOf(isAscending),
                jobFilter);

        ResultPagination<GetJobResponse> res = ResultPagination.<GetJobResponse>builder()
                .isSuccess(true)
                .message("Get jobs successfully")
                .httpStatus(HttpStatus.OK)
                .currentPage(jobResPage.getNumber() + 1)
                .pageSize(jobResPage.getSize())
                .previousPage(jobResPage.hasPrevious() ? jobResPage.getNumber() : null)
                .nextPage(jobResPage.hasNext() ? jobResPage.getNumber() + 2 : null)
                .data(jobResPage.getContent())
                .totalPage(jobResPage.getTotalPages())
                .totalElement(jobResPage.getTotalElements())
                .build();


        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping("/{jobId}/related")
    public ResponseEntity<ResponseDto> getRelatedJobs(@PathVariable Long jobId,
                                                       @RequestParam(name = "keyword") String keyword,
                                                       @RequestParam(defaultValue = "7") int limit) {

        List<GetJobResponse> relatedJob = jobService.getRelatedJob(jobId, keyword, limit);

        ResponseDto response = ResponseDto.builder()
                .status(HttpStatus.OK)
                .message("Get related job successfully")
                .isSuccess(true)
                .data(relatedJob)
                .build();


        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/save")
    public ResponseEntity<ResultPagination> getSavedJobsByUser(
                                                            @RequestParam(value = "currentPage", defaultValue = "1") int current,
                                                            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
                                                            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
                                                            @RequestParam(value = "ascending", defaultValue = "true") String isAscending) {


        Page<GetJobResponse> jobResPage = jobSaveService.getAllSavedJobsByUser(current - 1, pageSize, sortBy, Boolean.valueOf(isAscending));

        ResultPagination<GetJobResponse> res = ResultPagination.<GetJobResponse>builder()
                .isSuccess(true)
                .message("Get jobs successfully")
                .httpStatus(HttpStatus.OK)
                .currentPage(jobResPage.getNumber() + 1)
                .pageSize(jobResPage.getSize())
                .previousPage(jobResPage.hasPrevious() ? jobResPage.getNumber() : null)
                .nextPage(jobResPage.hasNext() ? jobResPage.getNumber() + 2 : null )
                .data(jobResPage.getContent())
                .totalPage(jobResPage.getTotalPages())
                .totalElement(jobResPage.getTotalElements())
                .build();


        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @DeleteMapping("/save")
    public ResponseEntity<ResponseDto> removeSavedJob(@RequestParam(value = "jobId") Long jobId) {

        jobSaveService.deleteSavedJob(jobId);

        ResponseDto response = ResponseDto.builder()
                .status(HttpStatus.OK)
                .message("Remove saved job successfully")
                .data(null)
                .isSuccess(true)
                .build();


        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @PutMapping("/status")
    public ResponseEntity<ResponseDto> updateJobStatus(@Valid @RequestBody UpdateJobStatusRequest request) {

        jobService.updateJobStatus(request);

        ResponseDto response = ResponseDto.builder()
                .isSuccess(true)
                .status(HttpStatus.OK)
                .message("Update job status successfully")
                .data(null)
                .build();


        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}