package com.example.Job.service.Impl;

import com.example.Job.constant.JobStatusEnum;
import com.example.Job.entity.*;
import com.example.Job.exception.ResourceNotFoundException;
import com.example.Job.models.dtos.*;
import com.example.Job.repository.CompanyRepository;
import com.example.Job.repository.JobRepository;
import com.example.Job.security.JwtUtil;
import com.example.Job.service.IApplyService;
import com.example.Job.service.IJobSaveService;
import com.example.Job.service.IJobService;
import com.example.Job.service.INotificationService;
import com.example.Job.specifications.JobOrdering;
import com.example.Job.specifications.JobSpecifications;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements IJobService {

    private final JobRepository jobRepository;
    private final ModelMapper modelMapper;
    private final CompanyRepository companyRepository;
    private final IJobSaveService jobSaveService;
    private final JwtUtil jwtUtil;
    private final IApplyService applyService;
    private final INotificationService notificationService;

    public JobServiceImpl(JobRepository jobRepository, ModelMapper modelMapper, CompanyRepository companyRepository, IJobSaveService jobSaveService, JwtUtil jwtUtil, IApplyService applyService, INotificationService notificationService) {
        this.jobRepository = jobRepository;
        this.modelMapper = modelMapper;
        this.companyRepository = companyRepository;
        this.jobSaveService = jobSaveService;

        this.jwtUtil = jwtUtil;
        this.applyService = applyService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public JobDetailResponse createJob(JobCreateRequest jobCreateRequest) {
        Company company = companyRepository.findById(jobCreateRequest.getCompanyId())
                .orElseThrow(() ->  new ResourceNotFoundException("Company", "id", jobCreateRequest.getCompanyId()));
//
//        Company company1 = new Company();
//        company1.setId(jobCreateRequest.getCompanyId());
//        Job newJob = modelMapper.map(jobCreateRequest, Job.class);
        Instant convertedDeadline = jobCreateRequest.getDeadline().atStartOfDay().toInstant(ZoneOffset.UTC);
        Job newJob = Job.builder()
                .jobType(jobCreateRequest.getJobType())
                .jobStatus(jobCreateRequest.getJobStatus() != null ? jobCreateRequest.getJobStatus() : JobStatusEnum.PENDING)
                .level(jobCreateRequest.getLevel())
                .description(jobCreateRequest.getDescription())
                .detail(jobCreateRequest.getDetail())
                .name(jobCreateRequest.getName())
                .quantity(jobCreateRequest.getQuantity())
                .salaryFrom(jobCreateRequest.getSalaryFrom())
                .salaryTo(jobCreateRequest.getSalaryTo())
                .yearOfExperience(jobCreateRequest.getYearOfExperience())
                .deadline(convertedDeadline)
                .city(jobCreateRequest.getCity())
                .company(company)
                .skills(jobCreateRequest.getSkills())
                .build();
//        newJob.setCompany(company);


        Job savedJob = jobRepository.save(newJob);

        List<String> followerIds = company.getCompanyFollowers().stream().map(companyFollow ->
                companyFollow.getId().getUserId().toString()).toList();

        // After create a new job, send notifications to followers
        System.out.println("Create job thread: " + Thread.currentThread().getName());

        String title = "Nhà tuyển dụng đã đăng việc làm mới";
        String message =  "Công ty " + company.getName() + " vừa đăng tin tuyển dụng mới. Vào xem ngay.";

        String link = "#";
        NotificationRequest notificationRequest = new NotificationRequest(message, title, link, savedJob.getCreatedAt());
        notificationService.sendNotificationToMultipleUser(followerIds, notificationRequest);

        return modelMapper.map(savedJob, JobDetailResponse.class);
    }

    @Override
    public JobDetailResponse updateJob(long id, JobDetailResponse jobDetailResponse) {
        return null;
    }

    @Override
    public Page<GetJobResponse> getAllJobs(int currentPage, int pageSize, String sortBy, boolean isAscending) {

        Sort sort = isAscending ? Sort.by(sortBy) : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sort);


        Page<Job> jobPage = jobRepository.findAllWithCompany(pageRequest);

//        Page<GetJobResponse> jobResPage = jobPage.map(job -> modelMapper.map(job, GetJobResponse.class));
//        ExampleMatcher matcher = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)

        Page<GetJobResponse> jobResPage = jobPage.map(job -> {
            return GetJobResponse.builder()
                    .id(job.getId())
                    .salaryFrom(job.getSalaryFrom())
                    .salaryTo(job.getSalaryTo())
                    .city(job.getCity())
                    .name(job.getName())
                    .yearOfExperience(job.getYearOfExperience())
                    .companyName(job.getCompany().getName())
                    .companyImg(job.getCompany().getImgUrl())
                    .companyId(job.getCompany().getId())
                    .createdAt(job.getCreatedAt())
                    .updatedAt(job.getUpdatedAt())
                    .build();
        });


        return jobResPage;
    }

    @Override
    public JobDetailResponse getJobDetailById(long id) {
        Job job = getJobById(id);
        JobDetailResponse response = modelMapper.map(job, JobDetailResponse.class);


        return response;
    }

    @Override
    public JobDetailCompanyResponse getJobDetailWithCompanyById(long id) {
        Job job = getJobById(id);

        JobDetailCompanyResponse response = modelMapper.map(job, JobDetailCompanyResponse.class);

        boolean isSaved = jobSaveService.isJobSaved(job.getId());
        response.setSaved(isSaved);

        JobApply apply = applyService.findJobApplyByJobId(job.getId());

        if(apply != null){
            response.setAppliedAt(apply.getCreatedAt());
        }
        return response;
    }

    @Override
    public Job getJobById(long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job", "id", id));
    }

    @Override
    public Page<GetJobResponse> getAllJobsByCompany(Long companyId, int currentPage, int pageSize, String sortBy, boolean isAscending) {
        Sort sort = isAscending ? Sort.by(sortBy) : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sort);


        Page<Job> jobPage = jobRepository.findJobByCompanyId( companyId, pageRequest);

//        Page<GetJobResponse> jobResPage = jobPage.map(job -> modelMapper.map(job, GetJobResponse.class));
//        ExampleMatcher matcher = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)

        Page<GetJobResponse> jobResPage = jobPage.map(job -> {
            return GetJobResponse.builder()
                    .id(job.getId())
                    .salaryFrom(job.getSalaryFrom())
                    .salaryTo(job.getSalaryTo())
                    .city(job.getCity())
                    .name(job.getName())
                    .createdAt(job.getCreatedAt())
                    .updatedAt(job.getUpdatedAt())
                    .yearOfExperience(job.getYearOfExperience())
                    .companyName(job.getCompany().getName())
                    .companyImg(job.getCompany().getImgUrl())
                    .companyId(job.getCompany().getId())
                    .build();
        });

        return jobResPage;
    }

    @Override
    public Page<JobPostResponse> getAllJobPostsByCompany(Long companyId,JobStatusEnum postStatus, int currentPage, int pageSize, String sortBy, boolean isAscending) {
        Sort sort = isAscending ? Sort.by(sortBy) : Sort.by(sortBy).descending();

        PageRequest pageRequest = PageRequest.of(currentPage, pageSize, sort);


        Page<Job> jobPage = jobRepository.findJobByCompanyIdWithStatus( companyId, postStatus,pageRequest);

//        Page<GetJobResponse> jobResPage = jobPage.map(job -> modelMapper.map(job, GetJobResponse.class));
//        ExampleMatcher matcher = ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)

        Page<JobPostResponse> jobResPage = jobPage.map(job -> {
            return modelMapper.map(job, JobPostResponse.class);
        });

        return jobResPage;
    }

    @Override
    public List<GetJobResponse> getRelatedJob(Long jobId, String title, int limit) {
//        List<GetJobResponseDto> relatedJobs = jobRepository.findRelatedJobsByKeyword(jobId, limit);

        Specification<Job> spec = (root, query, cb) -> {
            // Add filtering logic
            Predicate predicate = Specification
                    .where(JobSpecifications.hasSimilarKeywordWithTitle(title))
                    .toPredicate(root, query, cb);

            // Add ordering logic
            JobOrdering jobOrdering = new JobOrdering();

            jobOrdering.addRankOrder(root, cb, title);

            // Apply ordering
            jobOrdering.apply(query);

            return predicate;
        };

        Page<Job> jobPage = jobRepository.findAll(spec, PageRequest.of(0, limit));

        Page<GetJobResponse> jobResponses = jobPage.map(job -> modelMapper.map(job, GetJobResponse.class));

        return jobResponses.getContent().stream().
                filter(job -> job.getId() != jobId).collect(Collectors.toList());
    }


    @Override
    public void deleteJob(long id) {

    }

    @Override
    public Page<GetJobResponse> searchForJobs(int currentPage, int pageSize, String sortBy, boolean isAscending, JobFilter jobFilter) {
//        Sort sort = isAscending ? Sort.by(sortBy) : Sort.by(sortBy).descending();


        PageRequest pageRequest = PageRequest.of(currentPage, pageSize);

        Specification<Job> spec = (root, query, cb) -> {
            // Add fuzzy search, full-text search and filtering logic
            Predicate predicate = Specification
                    .where(JobSpecifications.hasKeyword(jobFilter.getKeyword())
                            .or(JobSpecifications.hasTitleMatchPartialWithSimilarity(jobFilter.getKeyword()))
                            .and(JobSpecifications.hasJobType(jobFilter.getJobType()))
                            .and(JobSpecifications.hasIndustry(jobFilter.getIndustry()))
                            .and(JobSpecifications.hasLevel(jobFilter.getLevel()))
                            .and(JobSpecifications.hasExperienceBetween(jobFilter.getMinExperience(), jobFilter.getMaxExperience()))
                            .and(JobSpecifications.hasSalaryBetween(jobFilter.getMinSalary(), jobFilter.getMaxSalary()))
                            .and(JobSpecifications.hasCityIn(jobFilter.getCities())))
                            .and(JobSpecifications.hasStatus(JobStatusEnum.ACTIVE))
                    .toPredicate(root, query, cb);

            // Add ordering logic
            JobOrdering jobOrdering = new JobOrdering();
            jobOrdering.addSortBy(root, cb, sortBy, isAscending);
            jobOrdering.addRankOrder(root, cb, jobFilter.getKeyword());

            // Apply ordering
            jobOrdering.apply(query);

            return predicate;
        };

//        Specification<Job> spec =  Specification.where(
//                    JobSpecifications.hasKeyword(jobFilter.getKeyword())
//                            .and(JobSpecifications.hasJobType(jobFilter.getJobType()))
//                            .and(JobSpecifications.hasIndustry(jobFilter.getIndustry()))
//                            .and(JobSpecifications.hasLevel(jobFilter.getLevel()))
//                            .and(JobSpecifications.hasExperienceBetween(jobFilter.getMinExperience(), jobFilter.getMaxExperience()))
//                            .and(JobSpecifications.hasSalaryBetween(jobFilter.getMinSalary(), jobFilter.getMaxSalary()))
//                            .and(JobSpecifications.hasCityIn(jobFilter.getCities()))
//                .and(JobSpecifications.sortBy(sortBy, isAscending))
//                .and(JobSpecifications.orderByRank(jobFilter.getKeyword()));



        Page<Job> jobPage = jobRepository.findAll(spec, pageRequest);


        return jobPage.map(job -> {
                GetJobResponse response = modelMapper.map(job, GetJobResponse.class);

                if(JwtUtil.isAuthenticated()){
                    boolean isSaved = jobSaveService.isJobSaved(job.getId());
                    response.setSaved(isSaved);
                }
                return response;
            }
        );
    }



    @Override
    @Transactional
    public void updateJobStatus(UpdateJobStatusRequest request) {
//        Job job = getJobById(request.getJobId());
//
//        job.setJobStatus(request.getJobStatus());

//        jobRepository.save(job);
        try{
            jobRepository.updateJobStatus(request.getJobStatus(), request.getJobId());
        } catch (RuntimeException e) {
            throw new RuntimeException(e.getMessage());
        }

    }
}
