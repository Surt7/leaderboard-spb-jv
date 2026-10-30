package com.worldpinball.leaderboard.application.features.iplicenses.Public.getalliplicenses;

import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Publisher;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.PublisherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GetAllIpLicensesHandlerImpl implements GetAllIpLicensesHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetAllIpLicensesHandlerImpl.class);

    private final IpLicenseRepository ipLicenseRepository;
    private final PublisherRepository publisherRepository;

    public GetAllIpLicensesHandlerImpl(IpLicenseRepository ipLicenseRepository, PublisherRepository publisherRepository) {
        this.ipLicenseRepository = ipLicenseRepository;
        this.publisherRepository = publisherRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GetAllIpLicensesResult handle(GetAllIpLicensesQuery query) {
        logger.info("[3300:FetchingIpLicenses] Fetching all IP licenses.");
        List<IpLicense> licenses = ipLicenseRepository.findAllByActiveTrueOrderByNameAsc();

        if (licenses.isEmpty()) {
            logger.warn("[3301:NoIpLicensesFound] No IP licenses found in the system.");
            return new GetAllIpLicensesResult.NotFound();
        }

        Set<UUID> publisherIds = licenses.stream()
                .map(IpLicense::getPublisherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, Publisher> publisherMap = publisherRepository.findAllById(publisherIds).stream()
                .collect(Collectors.toMap(Publisher::getPublisherId, p -> p));

        List<IpLicenseDto> dtos = licenses.stream()
                .map(lic -> {
                    Publisher pub = lic.getPublisherId() != null ? publisherMap.get(lic.getPublisherId()) : null;
                    PublisherDto pubDto = pub != null ? new PublisherDto(pub.getPublisherId(), pub.getName(), pub.getSlug()) : null;
                    return new IpLicenseDto(
                            lic.getIpId(),
                            lic.getName(),
                            lic.getSlug(),
                            pubDto
                    );
                })
                .toList();

        return new GetAllIpLicensesResult.Success(dtos);
    }
}
