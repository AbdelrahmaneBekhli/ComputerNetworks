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

	public static int getDistance(byte[] hash1, byte[] hash2) {
		byte b1 = 0;
		byte b2 = 0;
		int counter = 0;
		for (int i = 0; i < hash1.length; i++) {
			if (hash1[i] != hash2[i]) {
				b1 = hash1[i];
				b2 = hash2[i];
				break;
			} else {
				counter++;
			}
		}
		byte res = (byte)((b1 ^ b2));
		int numberDiff = numberOfLeadingZeros(res);
		return 256 - ((counter * 8) - 24 + numberDiff);
	}
}
