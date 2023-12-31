package ie.jules.salon.service;

import static ie.jules.salon.util.CSVParserUtil.parseAppointments;
import static ie.jules.salon.util.CSVParserUtil.parseClients;
import static ie.jules.salon.util.CSVParserUtil.parsePurchases;
import static ie.jules.salon.util.CSVParserUtil.parseServices;
import static ie.jules.salon.util.DatabaseUtil.saveOrUpdateAppointments;
import static ie.jules.salon.util.DatabaseUtil.saveOrUpdateClients;
import static ie.jules.salon.util.DatabaseUtil.saveOrUpdatePurchases;
import static ie.jules.salon.util.DatabaseUtil.saveOrUpdateServices;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import com.opencsv.exceptions.CsvValidationException;

import ie.jules.salon.enums.TableName;
import ie.jules.salon.model.ClientLoyaltyDto;
import ie.jules.salon.model.ImportedCsvJson;
import ie.jules.salon.model.entity.Appointment;
import ie.jules.salon.model.entity.Client;
import ie.jules.salon.model.entity.Purchase;
import ie.jules.salon.model.repository.AppointmentRepository;
import ie.jules.salon.model.repository.ClientRepository;
import ie.jules.salon.model.repository.PurchaseRepository;
import ie.jules.salon.model.repository.ServiceRepository;
import ie.jules.salon.util.DatabaseUtil;
import jakarta.transaction.Transactional;

@Service
public class SalonService {
	private final AppointmentRepository appointmentRepository;
	private final ClientRepository clientRepository;
	private final PurchaseRepository purchaseRepository;
	private final ServiceRepository serviceRepository;
	private final PlatformTransactionManager transactionManager;

	@Autowired
	public SalonService(AppointmentRepository appointmentRepository, ClientRepository clientRepository,
			PurchaseRepository purchaseRepository, ServiceRepository serviceRepository,
			PlatformTransactionManager transactionManager) {
		this.appointmentRepository = appointmentRepository;
		this.clientRepository = clientRepository;
		this.purchaseRepository = purchaseRepository;
		this.serviceRepository = serviceRepository;
		this.transactionManager = transactionManager;
	}

	public ResponseEntity<String> importCsvData(ImportedCsvJson importedCsvJson)
			throws IOException, CsvValidationException {
		TableName tableName = TableName.valueOf(importedCsvJson.getTableName().toUpperCase());
		List<Appointment> appointments = null;
		List<ie.jules.salon.model.entity.Service> services = null;
		List<Client> clients = null;
		List<Purchase> purchases = null;

		switch (tableName) {
			case CLIENTS -> clients = parseClients(importedCsvJson.getCsvData());
			case APPOINTMENTS -> appointments = parseAppointments(importedCsvJson.getCsvData());
			case PURCHASES -> purchases = parsePurchases(importedCsvJson.getCsvData());
			case SERVICES -> services = parseServices(importedCsvJson.getCsvData());
		}

		if (clients != null) {
			saveOrUpdateClients(clients, clientRepository, transactionManager);
		} else if (appointments != null) {
			saveOrUpdateAppointments(appointments, appointmentRepository, transactionManager);
		} else if (purchases != null) {
			saveOrUpdatePurchases(purchases, purchaseRepository, transactionManager);
		} else if (services != null) {
			saveOrUpdateServices(services, serviceRepository, transactionManager);
		} else {
			return new ResponseEntity<>("{\"response\":\"Could not read entities from CSV\"}",
					HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>("{\"response\":\"Import successful\"}", HttpStatus.OK);
	}

	@Transactional
	public List<Client> getAllClients() {
		return clientRepository.findAll();
	}

	@Transactional
	public Client getClientById(String id) {
		return clientRepository.findById(id).orElse(null);
	}

	@Transactional
	public Appointment getAppointmentById(String id) {
		return appointmentRepository.findById(id).orElse(null);
	}

	@Transactional
	public List<Appointment> findAppointmentsByClientId(String clientId) {
		return appointmentRepository.findByClientId(clientId).orElse(null);
	}

	@Transactional
	public Purchase getPurchaseById(String id) {
		return purchaseRepository.findById(id).orElse(null);
	}

	@Transactional
	public ie.jules.salon.model.entity.Service getServiceById(String id) {
		return serviceRepository.findById(id).orElse(null);
	}

	public List<ClientLoyaltyDto> findMostLoyalClients(LocalDate date, int limit) {
		LocalDateTime localDateTime = date.atStartOfDay();
		OffsetDateTime offsetDateTime = localDateTime.atOffset(ZoneOffset.UTC);
		return clientRepository.findTopClientsWithLoyaltyPoints(offsetDateTime, limit);
	}

	@Transactional
	public int deleteClientAndReferences(String id) {
		List<Appointment> appointments = appointmentRepository.findByClientId(id).orElse(null);
		int deletedEntities = 0;

		if (appointments != null) {
			for (Appointment appointment : appointments) {
				deletedEntities += purchaseRepository.deleteByAppointmentId(appointment.getId());
				deletedEntities += serviceRepository.deleteByAppointmentId(appointment.getId());
				deletedEntities += appointmentRepository.deleteAppointmentById(appointment.getId());
			}
		}
		deletedEntities += clientRepository.deleteClientById(id);
		return deletedEntities;
	}

	@Transactional
	public int deleteAppointment(String id) {
		return appointmentRepository.deleteAppointmentById(id);
	}

	@Transactional
	public int deleteService(String id) {
		return serviceRepository.deleteServiceById(id);
	}

	@Transactional
	public int deletePurchase(String id) {
		return purchaseRepository.deletePurchaseById(id);
	}

	@Transactional
	public int deleteClient(String id) {
		return clientRepository.deleteClientById(id);
	}

	@Transactional
	public Client updateClient(Client client) {
		return DatabaseUtil.updateClient(client, clientRepository);
	}
}
