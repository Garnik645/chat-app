package com.chatapp.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceClient {

    private final RestTemplate restTemplate;

    @Value("${room-service.url}")
    private String roomServiceUrl;

    /**
     * Returns true if the user is a member of the room.
     * Returns false if the user is not a member (404).
     * Throws RoomServiceUnavailableException if the service is unreachable or returns 5xx.
     */
    public boolean isMember(UUID roomId, UUID userId) {
        String url = roomServiceUrl + "/api/rooms/" + roomId + "/members/" + userId;
        try {
            ResponseEntity<Void> response = restTemplate.getForEntity(url, Void.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Room Service returned client error checking membership roomId={} userId={}: {}",
                    roomId, userId, e.getMessage());
            return false;
        } catch (ResourceAccessException e) {
            log.error("Room Service is unreachable when checking membership roomId={} userId={}: {}",
                    roomId, userId, e.getMessage());
            throw new RoomServiceUnavailableException("Room Service is unavailable");
        } catch (Exception e) {
            log.error("Unexpected error checking Room Service membership roomId={} userId={}: {}",
                    roomId, userId, e.getMessage());
            throw new RoomServiceUnavailableException("Room Service call failed");
        }
    }

    /**
     * Returns true if the user is the creator/admin of the room.
     * Throws RoomServiceUnavailableException if the service is unreachable or returns 5xx.
     */
    public boolean isRoomAdmin(UUID roomId, UUID userId) {
        String url = roomServiceUrl + "/api/rooms/" + roomId;
        try {
            ResponseEntity<RoomDto> response = restTemplate.getForEntity(url, RoomDto.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return userId.equals(response.getBody().getCreatedBy());
            }
            return false;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            log.error("Room Service returned client error checking admin roomId={} userId={}: {}",
                    roomId, userId, e.getMessage());
            return false;
        } catch (ResourceAccessException e) {
            log.error("Room Service is unreachable when checking admin roomId={} userId={}: {}",
                    roomId, userId, e.getMessage());
            throw new RoomServiceUnavailableException("Room Service is unavailable");
        } catch (Exception e) {
            log.error("Unexpected error checking Room admin roomId={} userId={}: {}",
                    roomId, userId, e.getMessage());
            throw new RoomServiceUnavailableException("Room Service call failed");
        }
    }

    /** Minimal DTO to extract createdBy from Room Service response */
    public static class RoomDto {
        private UUID createdBy;
        public UUID getCreatedBy() { return createdBy; }
        public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    }

    public static class RoomServiceUnavailableException extends RuntimeException {
        public RoomServiceUnavailableException(String message) {
            super(message);
        }
    }
}
