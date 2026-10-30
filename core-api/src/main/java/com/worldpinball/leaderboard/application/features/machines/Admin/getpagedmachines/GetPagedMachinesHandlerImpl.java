package com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines;

import com.worldpinball.leaderboard.application.dtos.FleetStatsDto;
import com.worldpinball.leaderboard.application.dtos.PagedResponse;
import com.worldpinball.leaderboard.application.features.machines.Admin.getallmachines.MachineRowDto;
import com.worldpinball.leaderboard.application.features.machines.Admin.getpagedmachines.queries.GetPagedMachinesQuery;
import com.worldpinball.leaderboard.domain.entities.DeviceCertificate;
import com.worldpinball.leaderboard.domain.entities.IpLicense;
import com.worldpinball.leaderboard.domain.entities.Machine;
import com.worldpinball.leaderboard.domain.entities.MachineDevice;
import com.worldpinball.leaderboard.domain.enums.MachineStatus;
import com.worldpinball.leaderboard.infrastructure.DeviceCertificateRepository;
import com.worldpinball.leaderboard.infrastructure.IpLicenseRepository;
import com.worldpinball.leaderboard.infrastructure.MachineDeviceRepository;
import com.worldpinball.leaderboard.infrastructure.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetPagedMachinesHandlerImpl implements GetPagedMachinesHandler {

    private final MachineRepository machineRepository;
    private final IpLicenseRepository ipLicenseRepository;
    private final MachineDeviceRepository machineDeviceRepository;
    private final DeviceCertificateRepository deviceCertificateRepository;

    @Override
    @Transactional(readOnly = true)
    public GetPagedMachinesResult handle(GetPagedMachinesQuery query) {
        List<Machine> allMachines = machineRepository.findAll();

        int totalFleet = allMachines.size();
        if (totalFleet == 0) {
            return new GetPagedMachinesResult.NotFound();
        }

        int online = 0;
        int offline = 0;
        int maintenance = 0;
        int pending = 0;

        for (Machine m : allMachines) {
            MachineStatus st = m.getStatus();
            if (st == MachineStatus.ACTIVE) {
                online++;
            } else if (st == MachineStatus.INACTIVE || st == MachineStatus.REVOKED) {
                offline++;
            } else if (st == MachineStatus.MAINTENANCE) {
                maintenance++;
            } else if (st == MachineStatus.PENDING_ENROLLMENT) {
                pending++;
            }
        }
        FleetStatsDto stats = new FleetStatsDto(totalFleet, online, offline, maintenance, pending);

        Set<UUID> ipIds = allMachines.stream()
                .map(Machine::getIpId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> ipNameMap = ipLicenseRepository.findAllById(ipIds).stream()
                .collect(Collectors.toMap(IpLicense::getIpId, IpLicense::getName));

        List<Machine> filtered = allMachines;
        if (query.search() != null && !query.search().isBlank()) {
            String term = query.search().trim().toLowerCase(Locale.ROOT);
            filtered = allMachines.stream()
                    .filter(m -> {
                        String serial = m.getWorldPinballSerialNumber() != null ? m.getWorldPinballSerialNumber().toLowerCase(Locale.ROOT) : "";
                        String display = m.getDisplayName() != null ? m.getDisplayName().toLowerCase(Locale.ROOT) : "";
                        String ipName = ipNameMap.getOrDefault(m.getIpId(), "").toLowerCase(Locale.ROOT);
                        return serial.contains(term) || display.contains(term) || ipName.contains(term);
                    })
                    .toList();
        }

        int totalCount = filtered.size();

        List<Machine> sorted = new ArrayList<>(filtered);
        sorted.sort(Comparator.comparing(Machine::getMachineId));

        if (query.lastMachineId() != null) {
            UUID lastId = query.lastMachineId();
            sorted = sorted.stream()
                    .filter(m -> m.getMachineId().compareTo(lastId) > 0)
                    .collect(Collectors.toList());
        } else if (query.pageNumber() != null && query.pageNumber() > 1) {
            int skip = (query.pageNumber() - 1) * query.pageSize();
            if (skip >= sorted.size()) {
                sorted = Collections.emptyList();
            } else {
                sorted = sorted.subList(skip, sorted.size());
            }
        }

        int fetchCount = Math.min(sorted.size(), query.pageSize() + 1);
        List<Machine> rawSlice = sorted.subList(0, fetchCount);

        boolean hasNextPage = rawSlice.size() > query.pageSize();
        List<Machine> pageMachines = hasNextPage ? rawSlice.subList(0, query.pageSize()) : rawSlice;

        List<MachineRowDto> items = pageMachines.stream()
                .map(m -> {
                    Instant certExpiresAt = null;
                    Integer certStatus = null;
                    if (machineDeviceRepository != null && deviceCertificateRepository != null) {
                        Optional<MachineDevice> activeDevice = machineDeviceRepository.findFirstByMachineIdAndActiveTrue(m.getMachineId());
                        if (activeDevice.isPresent()) {
                            Optional<DeviceCertificate> cert = deviceCertificateRepository.findFirstByDeviceIdOrderByIssuedAtUtcDesc(activeDevice.get().getDeviceId());
                            if (cert.isPresent()) {
                                certExpiresAt = cert.get().getNotAfterUtc();
                                certStatus = cert.get().getStatus().getValue();
                            }
                        }
                    }
                    return new MachineRowDto(
                            m.getMachineId(),
                            ipNameMap.getOrDefault(m.getIpId(), "Unknown IP"),
                            m.getDisplayName(),
                            m.getStatus(),
                            m.getMachineNotes(),
                            m.getRegisteredAtUtc(),
                            m.getUpdatedAtUtc(),
                            m.getWorldPinballSerialNumber(),
                            certExpiresAt,
                            certStatus
                    );
                })
                .toList();

        int pageNumber = query.pageNumber() != null ? query.pageNumber() : 1;
        boolean hasPreviousPage = query.lastMachineId() != null || pageNumber > 1;
        UUID lastId = items.isEmpty() ? null : items.get(items.size() - 1).machineId();

        PagedResponse<MachineRowDto> page = new PagedResponse<>(
                items,
                query.pageSize(),
                totalCount,
                pageNumber,
                hasPreviousPage,
                hasNextPage,
                lastId,
                stats
        );

        return new GetPagedMachinesResult.Success(page);
    }
}
