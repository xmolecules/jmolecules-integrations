package example;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.axonframework.queryhandling.QueryHandler;
import org.jmolecules.architecture.cqrs.QueryModel;
import org.jmolecules.event.annotation.DomainEventHandler;

@Slf4j
@QueryModel
public class SampleQueryModel {

	private final List<SampleEventOccurred> all = new ArrayList<>();

	@DomainEventHandler
	public void on(SampleEventOccurred event) {

		log.info("Event occurred" + event.toString());

		all.add(event);
	}

	@DomainEventHandler
	public void on(SampleRevokedEventOccurred event) {

		log.info("Revoke event occurred" + event.toString());

		findById(event.getIdentifier()).ifPresent(all::remove);
	}

	@QueryHandler
	public List<SampleEventOccurred> findByValue(String value) {
		return all.stream().filter(event -> event.getValue().equals(value)).collect(Collectors.toList());
	}

	@QueryHandler
	public Optional<SampleEventOccurred> findById(SampleAggregateIdentifier id) {
		return all.stream().filter(event -> event.getIdentifier().equals(id)).findFirst();
	}
}
