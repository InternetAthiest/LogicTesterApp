package se.assystems.LogicTester;

public class LogicTesterCommand{
    public byte sendByte;
    public LogicTesterCommandType commandType;
    public LogicTesterCommandEventListener listener;

    public LogicTesterCommand(LogicTesterCommandType commandType, byte sendByte, LogicTesterCommandEventListener listener){
        this.commandType = commandType;
        this.sendByte = sendByte;
        this.listener = listener;
    }

    public interface LogicTesterCommandEventListener{
        public void onCommandFinished(byte data);
    }

    public enum LogicTesterCommandType{
        SET_OUTPUT,
        GET_INPUT
    }
}
