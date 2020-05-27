package com.ivandelic.prototype.chaincodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.protobuf.ByteString;
import io.netty.handler.ssl.OpenSsl;

import org.apache.commons.lang3.SerializationUtils;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class JavaMeetupChaincode extends ChaincodeBase {

	private static final String MONEY = "money";
	private static final String PROPERTIES = "properties";

	@Override
	public Response init(ChaincodeStub stub) {
			return newSuccessResponse();
	}

	@Override
	public Response invoke(ChaincodeStub stub) {
		try {
			String func = stub.getFunction();
			List<String> params = stub.getParameters();
			
			if (func.equals("personRegistration"))
				return personRegistration(stub, params);
			if (func.equals("propertyRegistration"))
				return propertyRegistration(stub, params);
			if (func.equals("moneyDeposit"))
				return moneyDeposit(stub, params);
			if (func.equals("propertyTransaction"))
				return propertyTransaction(stub, params);
			if (func.equals("propertyQuery"))
				return propertyQuery(stub, params);
			if (func.equals("moneyQuery"))
				return moneyQuery(stub, params);
			if (func.equals("propertyQueryList"))
				return propertyQueryList(stub, params);
			
			return newErrorResponse("Invalid invoke function name.");
		} catch (Throwable e) {
			return newErrorResponse(e);
		}
	}
	
	private Response personRegistration(ChaincodeStub stub, List<String> args) {
		if (args.size() != 1)
			return newErrorResponse("Incorrect number of arguments. Expecting 1");
		
		try {
			String ownerPin = args.get(0);
			
			HashMap<String, Object> assets = new HashMap<String, Object>();
			assets.put(MONEY, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
			assets.put(PROPERTIES, new ArrayList<Property>());
			
			stub.putState(ownerPin, SerializationUtils.serialize(assets));
			
			return newSuccessResponse();
		} catch (Throwable e) {
			return newErrorResponse(e);
		}
	}

	private Response propertyRegistration(ChaincodeStub stub, List<String> args) {
		if (args.size() != 3)
			return newErrorResponse("Incorrect number of arguments. Expecting 3");
		
		try {
			String ownerPin = args.get(0);
			String landRegistryId = args.get(1);
			String propertyId = args.get(2);
			
			Property property = new Property(landRegistryId, propertyId);
			
			byte[] readBytes = stub.getState(ownerPin);
			HashMap<String, Object> assets = SerializationUtils.deserialize(readBytes);
			
			List<Property> properties = (List<Property>) assets.get(PROPERTIES);
			properties.add(property);

			assets.put(PROPERTIES, properties);

			stub.putState(ownerPin, SerializationUtils.serialize(assets));

			return newSuccessResponse();
		} catch (Throwable e) {
			return newErrorResponse(e);
		}
	}
	
	private Response moneyDeposit(ChaincodeStub stub, List<String> args) {
		if (args.size() != 2)
			return newErrorResponse("Incorrect number of arguments. Expecting 3");
		
		try {
			String ownerPin = args.get(0);
			String depositAmount = args.get(1);
			
			BigDecimal deposit = (new BigDecimal(depositAmount)).setScale(2, RoundingMode.HALF_EVEN);
			
			byte[] readBytes = stub.getState(ownerPin);
			HashMap<String, Object> assets = SerializationUtils.deserialize(readBytes);
			BigDecimal totalAmount = (BigDecimal) assets.get(MONEY);
			
			totalAmount = totalAmount.add(deposit);
			
			assets.put(MONEY, totalAmount);
			
			stub.putState(ownerPin, SerializationUtils.serialize(assets));

			return newSuccessResponse();
		} catch (Throwable e) {
			return newErrorResponse(e);
		}
	}

	private Response propertyTransaction(ChaincodeStub stub, List<String> args) {
		if (args.size() != 5)
            return newErrorResponse("Incorrect number of arguments. Expecting 4");
		
		String sellerPin = args.get(0);
		String buyerPin = args.get(1);
		String landRegistryId = args.get(2);
		String propertyId = args.get(3);
		String price = args.get(4);
		
		Property property = new Property(landRegistryId, propertyId);
		BigDecimal propertyPrice = (new BigDecimal(price)).setScale(2, RoundingMode.HALF_EVEN);
		
		byte[] sellerBytes = stub.getState(sellerPin);
		byte[] buyerBytes = stub.getState(buyerPin);
		
		if (sellerBytes == null || sellerBytes.length <= 0)
			return newErrorResponse(String.format("Seller %s not found", sellerPin));
		if (buyerBytes == null || buyerBytes.length <= 0)
			return newErrorResponse(String.format("Buyer %s not found", buyerBytes));
		
		HashMap<String, Object> sellerAssets = SerializationUtils.deserialize(sellerBytes);
		HashMap<String, Object> buyerAssets = SerializationUtils.deserialize(buyerBytes);
		
		List<Property> sellerProperties = (List<Property>) sellerAssets.get(PROPERTIES);
		List<Property> buyerProperties = (List<Property>) buyerAssets.get(PROPERTIES);
		
		if (!sellerProperties.contains(property))
			return newErrorResponse(String.format("Seller %s does not own the property %s", sellerPin, property.toString()));

		BigDecimal buyerFunds = (BigDecimal) buyerAssets.get(MONEY);
		BigDecimal sellerFunds = (BigDecimal) sellerAssets.get(MONEY);
		
		if (buyerFunds.compareTo(propertyPrice) < 0) {
			return newErrorResponse(String.format("Buyer %s does not have enough money. Current total amount is %s", buyerPin, buyerFunds.toString()));
		}

		buyerProperties.add(property);
		buyerFunds = buyerFunds.subtract(propertyPrice);
		buyerAssets.put(MONEY, buyerFunds);
		buyerAssets.put(PROPERTIES, buyerProperties);
		stub.putState(buyerPin, SerializationUtils.serialize(buyerAssets));
		
		sellerProperties.remove(property);
		sellerFunds = sellerFunds.add(propertyPrice);
		sellerAssets.put(MONEY, sellerFunds);
		sellerAssets.put(PROPERTIES, sellerProperties);
		stub.putState(sellerPin, SerializationUtils.serialize(sellerAssets));

        return newSuccessResponse("Property transaction finished successfully", ByteString.copyFrom(sellerPin + " -> " + buyerPin, UTF_8).toByteArray());
	}

	private Response propertyQuery(ChaincodeStub stub, List<String> args) {
		if (args.size() != 1)
			return newErrorResponse("Incorrect number of arguments. Expecting name of the person to query");
		
		String ownerPin = args.get(0);
		
		byte[] readBytes = stub.getState(ownerPin);
		if (readBytes == null || readBytes.length <= 0)
			return newErrorResponse(String.format("Owner %s not found", ownerPin));
		
		HashMap<String, Object> assets = SerializationUtils.deserialize(readBytes);
		List<Property> properties = (List<Property>) assets.get(PROPERTIES);

		return newSuccessResponse("Total properties count", ByteString.copyFrom(String.valueOf(properties != null ? properties.size() : 0), UTF_8).toByteArray());
	}
	
	private Response moneyQuery(ChaincodeStub stub, List<String> args) {
		if (args.size() != 1)
			return newErrorResponse("Incorrect number of arguments. Expecting name of the person to query");
		
		String ownerPin = args.get(0);
		
		byte[] readBytes = stub.getState(ownerPin);
		if (readBytes == null || readBytes.length <= 0)
			return newErrorResponse(String.format("Owner %s not found", ownerPin));
		
		HashMap<String, Object> assets = SerializationUtils.deserialize(readBytes);
		BigDecimal money = (BigDecimal) assets.get(MONEY);

		return newSuccessResponse("Total money deposit", ByteString.copyFrom(money.toPlainString(), UTF_8).toByteArray());
	}
	
	private Response propertyQueryList(ChaincodeStub stub, List<String> args) {
		if (args.size() != 1)
			return newErrorResponse("Incorrect number of arguments. Expecting name of the person to query");
		
		String ownerPin = args.get(0);
		
		byte[] readBytes = stub.getState(ownerPin);
		if (readBytes == null || readBytes.length <= 0)
			return newErrorResponse(String.format("Owner %s not found", ownerPin));
		
		HashMap<String, Object> assets = SerializationUtils.deserialize(readBytes);
		List<Property> properties = (List<Property>) assets.get(PROPERTIES);
		
		String ownerPropertiesFlat = "";
		
		if (properties != null && properties.size() > 0)
			for (int i = 0; i < properties.size(); i++) {
				if (i > 0)
					ownerPropertiesFlat += ",";
				ownerPropertiesFlat += properties.get(i).getLandRegistry() + "-" + properties.get(i).getProperty();
			}

		return newSuccessResponse("Flat list of properties", ByteString.copyFrom(ownerPropertiesFlat, UTF_8).toByteArray());
	}

	public static void main(String[] args) {
		System.out.println("OpenSSL avaliable: " + OpenSsl.isAvailable());
		new JavaMeetupChaincode().start(args);
	}
}
