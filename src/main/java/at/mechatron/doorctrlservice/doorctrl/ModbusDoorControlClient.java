package at.mechatron.doorctrlservice.doorctrl;

import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.exception.ModbusTransportException;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.WriteRegisterRequest;
import com.serotonin.modbus4j.msg.WriteRegisterResponse;

public class ModbusDoorControlClient implements DoorControlClient {
    private final int _slaveId = 1;
    private final ModbusMaster _master;

    public ModbusDoorControlClient(final String host) {
        IpParameters ipParameters = new IpParameters();
        ipParameters.setHost(host);

        ModbusFactory modbusFactory = new ModbusFactory();
        _master = modbusFactory.createTcpMaster(ipParameters, false);
    }

    public void lockDoor() {
        try {
            WriteRegisterRequest request = new WriteRegisterRequest(_slaveId, 0, 1);
            WriteRegisterResponse response = (WriteRegisterResponse) _master.send(request);

            if (response.isException())
                System.out.println("Exception response: message=" + response.getExceptionMessage());
            else
                System.out.println("Success");
        }
        catch (ModbusTransportException e) {
        }
    }

    public void unlockDoor() {

    }
}
