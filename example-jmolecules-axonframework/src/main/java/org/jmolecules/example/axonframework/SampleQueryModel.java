package org.jmolecules.example.axonframework;

import org.axonframework.queryhandling.QueryHandler;
import org.jmolecules.architecture.cqrs.annotation.QueryModel;
import org.jmolecules.event.annotation.DomainEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@QueryModel public class SampleQueryModel {

	private static final Logger LOG = LoggerFactory.getLogger(SampleQueryModel.class);

	private final List<SampleEventOccurred> all = new ArrayList<>();

	@DomainEventHandler public void on(SampleEventOccurred event) {
		LOG.info("Event occurred" + event.toString());
		all.add(event);
	}

	@DomainEventHandler public void on(SampleRevokedEventOccurred event) {
		LOG.info("Revoke event occurred" + event.toString());
		findById(event.getIdentifier()).ifPresent(all::remove);
	}

	@QueryHandler public List<SampleEventOccurred> findByValue(String value) {
		return all.stream().filter(event -> event.getValue().equals(value)).collect(Collectors.toList());
	}

	@QueryHandler public Optional<SampleEventOccurred> findById(SampleAggregateIdentifier id) {
		return all.stream().filter(event -> event.getIdentifier().equals(id)).findFirst();
	}

}
