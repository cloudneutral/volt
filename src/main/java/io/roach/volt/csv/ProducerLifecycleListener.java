package io.roach.volt.csv;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import io.roach.volt.csv.event.AbstractEventPublisher;
import io.roach.volt.csv.event.CompletionEvent;
import io.roach.volt.csv.event.GenericEvent;
import io.roach.volt.csv.event.ProducerCancelledEvent;
import io.roach.volt.csv.event.ProducerCompletedEvent;
import io.roach.volt.csv.event.ProducerFailedEvent;
import io.roach.volt.csv.event.ProducerProgressEvent;
import io.roach.volt.csv.event.ProducerStartedEvent;
import io.roach.volt.csv.model.Table;
import io.roach.volt.shell.support.AnsiConsole;
import io.roach.volt.util.ByteUtils;

@Component
public class ProducerLifecycleListener extends AbstractEventPublisher {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<Table> activeTables = Collections.synchronizedList(new ArrayList<>());

    @Autowired
    private AnsiConsole console;

    @EventListener
    public void onStartedEvent(GenericEvent<ProducerStartedEvent> event) {
        ProducerStartedEvent startedEvent = event.getTarget();

        activeTables.add(startedEvent.getTable());

        if (startedEvent.isBoundedCount()) {
            logger.info("Started producing '%s': %,d rows using '%s'".formatted(
                    startedEvent.getPath(),
                    startedEvent.getTable().getFinalCount(),
                    startedEvent.getProducerInfo()));
        } else {
            logger.info("Started producing '%s': ∞ rows using '%s'".formatted(
                    startedEvent.getPath(),
                    startedEvent.getProducerInfo()));
        }
    }

    @EventListener
    public void onProgressEvent(GenericEvent<ProducerProgressEvent> event) {
        ProducerProgressEvent pe = event.getTarget();
        if (pe.getTotal() > 0) {
            console.progressBar(
                    pe.getPosition(),
                    pe.getTotal(),
                    pe.getLabel(),
                    pe.getRequestsPerSec(),
                    pe.remainingMillis());
        }
    }

    @EventListener
    public synchronized void onCompletedEvent(GenericEvent<ProducerCompletedEvent> event) {
        if (activeTables.isEmpty()) {
            return;
        }

        activeTables.remove(event.getTarget().getTable());

        logger.info("Completed producing '%s': %,d rows in %s (%.0f/s avg) (%s) - %d in queue".formatted(
                event.getTarget().getPath(),
                event.getTarget().getRows(),
                event.getTarget().getDuration(),
                event.getTarget().calcRequestsPerSec(),
                ByteUtils.byteCountToDisplaySize(event.getTarget().getPath().toFile().length()),
                activeTables.size())
        );

        if (activeTables.isEmpty()) {
            publishEvent(new CompletionEvent());
        }
    }

    @EventListener
    public void onCancelledEvent(GenericEvent<ProducerCancelledEvent> event) {
        logger.warn("Cancelled producing table'%s'"
                .formatted(event.getTarget().getTable()));
    }

    @EventListener
    public void onFailedEvent(GenericEvent<ProducerFailedEvent> event) {
        logger.error("Failed to produce table '%s': %s"
                .formatted(event.getTarget().getTable(), event.getTarget().getCause()));
    }

    @EventListener
    public void onCompletionEvent(GenericEvent<CompletionEvent> event) {
        logger.info("Successfully produced all file(s)");
    }
}
