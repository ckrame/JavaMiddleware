package de.i2ar.ctrlbox.ws;

public class WSIO {

	private WSMessage input;
	private WSMessage output;
	
	public WSIO() {
		input = new WSMessage();
		output = new WSMessage();
	}
	
	public WSIO(WSMessage input) {
		this.input = input;
	}
	
	public WSIO(WSMessage input, WSMessage output) {
		this.input = input;
		this.output = output;
	}

	public WSMessage getInput() { return input; }
	
	public WSMessage getOutput() { return output; }

	public void setInput(WSMessage input) { this.input = input; }

	public void setOutput(WSMessage output) { this.output = output; }
	
}
