package com.docqueue.queue.service;

import com.docqueue.appointment.entity.Appointment;
import com.docqueue.clinic.entity.Clinic;
import com.docqueue.doctor.entity.Doctor;
import com.docqueue.queue.dto.QueueUpdateEvent;
import com.docqueue.queue.entity.QueueEntry;
import com.docqueue.queue.entity.QueueEntry.QueueStatus;
import com.docqueue.queue.repository.QueueEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class QueueServiceTest {

    @Mock private QueueEntryRepository queueEntryRepository;
    @Mock private QueueBroadcastService broadcastService;

    @InjectMocks
    private QueueService queueService;

    private Doctor doctor;
    private Clinic clinic;
    private Appointment appointment;

    @BeforeEach
    public void setUp() {
        clinic = Clinic.builder().id(1L).name("Test Clinic").build();
        doctor = Doctor.builder().id(2L).avgConsultMin(15).clinic(clinic).build();
        appointment = Appointment.builder().id(3L).doctor(doctor).clinic(clinic).tokenNumber(1).build();
    }

    @Test
    public void enqueue_Success() {
        when(queueEntryRepository.findByDoctorIdAndStatusOrderByQueuePositionAsc(2L, QueueStatus.WAITING))
                .thenReturn(new ArrayList<>());
        
        when(queueEntryRepository.findActiveQueueByDoctor(2L)).thenReturn(new ArrayList<>());

        QueueEntry result = queueService.enqueue(appointment);

        assertNotNull(result);
        assertEquals(1, result.getQueuePosition());
        assertEquals(15, result.getEstimatedWait());
        assertEquals(QueueStatus.WAITING, result.getStatus());

        verify(queueEntryRepository).save(any(QueueEntry.class));
        verify(broadcastService).broadcastToDoctor(eq(2L), any(QueueUpdateEvent.class));
    }

    @Test
    public void callNext_Success_WithWaiting() {
        QueueEntry currentInProgress = QueueEntry.builder()
                .id(10L)
                .appointment(appointment)
                .doctor(doctor)
                .status(QueueStatus.IN_PROGRESS)
                .build();

        Appointment nextAppt = Appointment.builder().id(4L).doctor(doctor).tokenNumber(2).build();
        QueueEntry nextWaiting = QueueEntry.builder()
                .id(11L)
                .appointment(nextAppt)
                .doctor(doctor)
                .status(QueueStatus.WAITING)
                .queuePosition(1)
                .build();

        List<QueueEntry> waitingList = new ArrayList<>();
        waitingList.add(nextWaiting);

        when(queueEntryRepository.findByDoctorIdAndStatus(2L, QueueStatus.IN_PROGRESS))
                .thenReturn(Optional.of(currentInProgress));
        when(queueEntryRepository.findByDoctorIdAndStatusOrderByQueuePositionAsc(2L, QueueStatus.WAITING))
                .thenReturn(waitingList);

        // Active queue list for broadcast
        List<QueueEntry> activeQueue = new ArrayList<>();
        activeQueue.add(nextWaiting); // now promoted
        when(queueEntryRepository.findActiveQueueByDoctor(2L)).thenReturn(activeQueue);

        queueService.callNext(2L);

        assertEquals(QueueStatus.COMPLETED, currentInProgress.getStatus());
        assertNotNull(currentInProgress.getCompletedAt());

        assertEquals(QueueStatus.IN_PROGRESS, nextWaiting.getStatus());
        assertNotNull(nextWaiting.getCalledAt());

        verify(queueEntryRepository).save(currentInProgress);
        verify(queueEntryRepository).save(nextWaiting);
        verify(broadcastService).broadcastToDoctor(eq(2L), any(QueueUpdateEvent.class));
    }

    @Test
    public void skipPatient_Success() {
        QueueEntry waitingEntry = QueueEntry.builder()
                .id(15L)
                .appointment(appointment)
                .doctor(doctor)
                .queuePosition(2)
                .status(QueueStatus.WAITING)
                .build();

        when(queueEntryRepository.findByAppointmentId(3L)).thenReturn(Optional.of(waitingEntry));
        when(queueEntryRepository.findActiveQueueByDoctor(2L)).thenReturn(new ArrayList<>());
        when(queueEntryRepository.findByDoctorIdAndStatusOrderByQueuePositionAsc(2L, QueueStatus.WAITING))
                .thenReturn(new ArrayList<>());

        queueService.skipPatient(3L, 2L);

        assertEquals(QueueStatus.SKIPPED, waitingEntry.getStatus());
        verify(queueEntryRepository).save(waitingEntry);
        verify(queueEntryRepository).decrementPositionsAfter(2L, 2);
        verify(broadcastService).broadcastToDoctor(eq(2L), any(QueueUpdateEvent.class));
    }

    @Test
    public void cancelQueueEntry_Success() {
        QueueEntry entry = QueueEntry.builder()
                .id(20L)
                .appointment(appointment)
                .doctor(doctor)
                .queuePosition(1)
                .status(QueueStatus.WAITING)
                .build();

        when(queueEntryRepository.findByAppointmentId(3L)).thenReturn(Optional.of(entry));
        when(queueEntryRepository.findActiveQueueByDoctor(2L)).thenReturn(new ArrayList<>());
        when(queueEntryRepository.findByDoctorIdAndStatusOrderByQueuePositionAsc(2L, QueueStatus.WAITING))
                .thenReturn(new ArrayList<>());

        queueService.cancelQueueEntry(3L);

        assertEquals(QueueStatus.CANCELLED, entry.getStatus());
        verify(queueEntryRepository).save(entry);
        verify(queueEntryRepository).decrementPositionsAfter(2L, 1);
        verify(broadcastService).broadcastToDoctor(eq(2L), any(QueueUpdateEvent.class));
    }
}
