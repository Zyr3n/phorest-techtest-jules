package ie.jules.salon.model.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ie.jules.salon.model.entity.Appointment;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
	Optional<Appointment> findById(String id);

	List<Appointment> findByStartTimeBetween(OffsetDateTime startTime, OffsetDateTime endTime);

	Optional<List<Appointment>> findByClientId(String clientId);
}
