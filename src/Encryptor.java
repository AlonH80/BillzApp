import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class Encryptor {

    private final int saltSize = 6;

    public String getEncryptedPassword(String pwd, String salt) {
        return encrypt(pwd + salt);
    }

    public String generateSalt() {
        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        char[] specialChars = {'!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '-', '+', '=', '{', '}', '|', '>', '<', '?', ',', '.', ';'};
        StringBuilder salt = new StringBuilder();
        char currRandChar = '0';
        int currCategory, currRandNum;
        while(salt.length() < saltSize) {
            currRandNum = rand.nextInt();
            currRandNum = Math.abs(currRandNum);
            currCategory = rand.nextInt() % 16;
            switch (currCategory){
                case 0:
                case 3:
                case 14:
                    currRandChar = (char)((int)'0' + currRandNum % 10);
                    break;
                case 2:
                case 7:
                case 9:
                case 13:
                    currRandChar = (char)((int)'a' + currRandNum % 26);
                    break;
                case 1:
                case 4:
                case 8:
                case 15:
                    currRandChar = (char)((int)'A' + currRandNum % 26);
                    break;
                case 5:
                case 6:
                case 10:
                case 11:
                case 12:
                    currRandChar = specialChars[currRandNum % specialChars.length];
                    break;
            }
            if (salt.toString().indexOf(currRandChar) == -1){
                salt.append(currRandChar);
            }
        }

        return salt.toString();
    }

    private String encrypt(String input)
    {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);

            String hashtext = no.toString(16);

            return hashtext;
        }

        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }



}
