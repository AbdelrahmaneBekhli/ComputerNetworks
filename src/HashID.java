// IN2011 Computer Networks
// Coursework 2023/2024
//
// Construct the hashID for a string

import java.lang.StringBuilder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

import static java.lang.Integer.numberOfLeadingZeros;

public class HashID {

	public static byte[] computeHashID(String line) throws Exception {
		if (line.endsWith("\n")) {
			// What this does and how it works is covered in a later lecture
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(line.getBytes(StandardCharsets.UTF_8));
			return md.digest();

		} else {
			// 2D#4 computes hashIDs of lines, i.e. strings ending with '\n'
			throw new Exception("No new line at the end of input to HashID");
		}
	}

	public static int getDistance(String Hexvalue1, String Hexvalue2) {
		char char1 = ' ';
		char char2 = ' ';
		int counter = 0;

		for (int i = 0; i < Hexvalue1.length(); i++) {
			if (Hexvalue1.charAt(i) != Hexvalue2.charAt(i)) {
				char1 = Hexvalue1.charAt(i);
				char2 = Hexvalue2.charAt(i);
				break;
			} else {
				counter++;
			}
		}
		if (char1 != ' ' & char2 != ' ') {
			// Convert hexadecimal characters to their integer values
			int intChar1 = Character.digit(char1, 16);
			int intChar2 = Character.digit(char2, 16);

			// Perform XOR operation
			int result = intChar1 ^ intChar2;
			StringBuilder binaryResult = new StringBuilder(Integer.toBinaryString(result));
			int zeros = 0;
			while (binaryResult.length() < 4) {
				binaryResult.insert(0, "0");
				zeros++;
			}
			return 256 - ((counter * 4) + zeros);

		}
		return 0;
	}

	public static String hexHash(String val){
		StringBuilder hexString = new StringBuilder();
		try {
			// Compute the hash of the input string 'val' using the 'computeHashID' method
			byte[] hashedData = HashID.computeHashID(val);
			for (byte b : hashedData) {
				// Convert the byte to a hexadecimal string
				String hex = Integer.toHexString(0xff & b);
				//if byte value < 16
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return hexString.toString();
	}
}
