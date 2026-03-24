package com.scopeflow.config;

import com.scopeflow.core.domain.briefing.*;
import com.scopeflow.core.domain.proposal.*;
import com.scopeflow.core.domain.user.UserRepository;
import com.scopeflow.core.domain.user.UserService;
import com.scopeflow.core.domain.workspace.WorkspaceMemberRepository;
import com.scopeflow.core.domain.workspace.WorkspaceRepository;
import com.scopeflow.core.domain.workspace.WorkspaceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring beans for domain services.
 *
 * Domain services are plain Java classes (no @Service annotation in domain layer).
 * We wire them here so they can be injected into controllers and adapters.
 */
@Configuration
public class DomainServiceConfig {

    @Bean
    public BriefingService briefingService(
            BriefingSessionRepository sessionRepository,
            BriefingQuestionRepository questionRepository,
            BriefingAnswerRepository answerRepository,
            AIGenerationRepository aiGenerationRepository
    ) {
        return new BriefingService(sessionRepository, questionRepository, answerRepository, aiGenerationRepository);
    }

    @Bean
    public UserService userService(UserRepository userRepository) {
        return new UserService(userRepository);
    }

    @Bean
    public WorkspaceService workspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository
    ) {
        return new WorkspaceService(workspaceRepository, memberRepository);
    }

    @Bean
    public ProposalService proposalService(
            ProposalRepository proposalRepository,
            ProposalVersionRepository versionRepository,
            ApprovalWorkflowRepository workflowRepository
    ) {
        return new ProposalService(proposalRepository, versionRepository, workflowRepository);
    }
}
