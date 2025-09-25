
public class UserInput
{
    public static String getValidInput(String[] validInputs)
    {
        String input = "";
        boolean valid;
        do
        {
            input = getLine().trim().toLowerCase();
            valid = false;  // reset for each loop
            for(String str : validInputs)
            {
                if(input.equals(str.toLowerCase()))
                    valid = true;
            }
            if(!valid)
                System.out.print("Invalid input. Please try again\n>");
        } while(!valid);
        return input;
    }

    public static String getLine() {
    String polled = null;
    while (polled == null) {
        polled = EscapeRoom.inputQueue.poll();
        try {
            Thread.sleep(50); // small delay to avoid busy-waiting
        } catch (InterruptedException e) {}
    }
    return polled;
    }  
}
