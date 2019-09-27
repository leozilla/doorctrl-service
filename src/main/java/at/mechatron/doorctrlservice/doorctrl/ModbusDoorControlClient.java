package at.mechatron.doorctrlservice.doorctrl;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.WriteRegisterRequest;
import com.serotonin.modbus4j.msg.WriteRegisterResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ModbusDoorControlClient implements DoorControlClient {
    private static final Logger LOG = LogManager.getLogger();

    private static final int SLAVE_ID = 1;

    private final ModbusMaster master;
    private final Executor ioExecutor;

    public ModbusDoorControlClient(final String host, final Executor ioExecutor) {
        this.ioExecutor = ioExecutor;

        IpParameters ipParameters = new IpParameters();
        ipParameters.setHost(host);

        ModbusFactory modbusFactory = new ModbusFactory();
        this.master = modbusFactory.createTcpMaster(ipParameters, false);
    }

    public CompletableFuture<Void> lockDoor() {
        LOG.debug("Locking door");
        return sendRequest(1);
    }

    public CompletableFuture<Void> unlockDoor() {
        LOG.debug("Unlocking door");
        return sendRequest(0);
    }

    private CompletableFuture<Void> sendRequest(final int writeValue) {
        final String doorState = writeValue == 1 ? "LOCKED" : "UNLOCKED";

        return CompletableFuture.runAsync(() -> {
            try {
                WriteRegisterRequest request = new WriteRegisterRequest(SLAVE_ID, 0, writeValue);
                WriteRegisterResponse response = (WriteRegisterResponse) this.master.send(request);

                if (!response.isException()) {
                    LOG.info("Door {}", doorState);
                } else {
                    LOG.fatal("Modbus write error. Door could not be {}. Exception Code: {} Message: {}", doorState, response.getExceptionCode(), response.getExceptionMessage());
                }
            }
            catch (ModbusTransportException e) {
                LOG.fatal("Modbus transport failed. Door could not be {}", doorState, e);
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }
}
