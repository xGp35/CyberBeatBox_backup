import javax.sound.midi.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

import static javax.sound.midi.ShortMessage.*;

public class BeatBox {
    private JList<String> incomingList;
    // This is a list to be displayed on the UI. This value needs to hydrate using an actual 
    // data structure. Swing uses Vector to hydrate this UI element. so in line 259 we do
    // incomingList.setListData(listVector);
    private JTextArea userMessage;
    private ArrayList<JCheckBox> checkboxList;

    private Vector<String> listVector = new Vector<>();
    // This is the actual list of String to be displayed on th JList list. This is just a java 
    // datastruture to store the text to be dispalyed. Why vector and not ArrayList ? - Swing is old
    private HashMap<String, boolean[]> otherSeqsMap = new HashMap<>();
    // Maps Username to thier checkboxSate, basically username to their music. But its not
    // only the username, we also append a num to it to make sure one user can have multiple tracks.

    private String userName;
    private int nextNum;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private JFrame frame;

    private static final int NUM_INSTRUMENTS = 16;
    private static final int NUM_BEATS = 20;

    String [] instrumentNames = {"Bass Drum", "Closed Hi-Hat", "Open Hi-Hat", "Acoustic Snare", 
    "Crash Cymbal", "Hand Clap", "High Tom", "Hi Bongo", "Maracas", "Whistle", "Low Conga", "Cowbell", 
    "Vibraslap", "Low-mid Tom", "High Agogo", "Open Hi Conga"};

    int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "youNameless";
        new BeatBox().startUp(name);
    }

    public void startUp(String name) {
        userName = name;

        // open connection to server
        try {
            Socket socket = new Socket("127.0.0.1", 4242);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            // We're using sockets instead of channels because they work better
            // with Object Input/Output streams
            ExecutorService executor = Executors.newSingleThreadExecutor();
            // because i need jsut one extra thread apart from main.
            executor.submit(new RemoteReader());
        } catch (Exception ex) {
            System.out.println("Couldn't connect - you'll have to play alone");
            // Executor code could have been kept outside as exception is thrown by I/O operations
            // But if connection fails, there's no need to make a thread of readers to read
        }

        setUpMidi();
        buildGUI();
    }

    public void buildGUI() {
        frame = new JFrame("Cyber BeatBox");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BorderLayout layout = new BorderLayout();
        JPanel background = new JPanel(layout);
        background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        // An "empty border" gives us a margin betweene edges of a panel and where
        // the components are p;aced. Purely Aesthetic

        Box buttonBox = new Box(BoxLayout.Y_AXIS);

        JButton start = new JButton("Start");
        start.addActionListener(e -> buildTrackAndStart());
        buttonBox.add(start);
        //Lambda expressions are perfect for these event handlers, since when these buttons
        //are pressed, all we want to do is call a specific method

        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> sequencer.stop());
        buttonBox.add(stop);

        JButton upTempo = new JButton("Tempo Up");
        upTempo.addActionListener(e -> changeTempo(1.03f));
        buttonBox.add(upTempo);
        //The default tempo is 1.0, so we’re adjusting +/- 3% per click.
        JButton downTempo = new JButton("Tempo Down");
        downTempo.addActionListener(e -> changeTempo(0.97f));
        buttonBox.add(downTempo);

        // Send message and current beat sequence to the music server
        JButton sendIt = new JButton("Send It");
        sendIt.addActionListener(e -> sendMessageAndTracks()); 
        // TODO - implement this in BeatBoxNetworking class
        buttonBox.add(sendIt);

        // Create a text Area for user to type their message
        userMessage = new JTextArea();
        userMessage.setLineWrap(true);
        userMessage.setWrapStyleWord(true);
        JScrollPane messageScroller = new JScrollPane(userMessage);
        buttonBox.add(messageScroller);


        incomingList = new JList<>();
        incomingList.addListSelectionListener(new MyListSelectionListener()); 
        // This is a event, It could have been lambda but as its ery big so implemeted as inner class
        incomingList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane theList = new JScrollPane(incomingList);
        buttonBox.add(theList);
        incomingList.setListData(listVector);

        // Left side element, i.e, list of all instrument names
        // Box nameBox = new Box(BoxLayout.Y_AXIS);
        // I am changing nameBoc to be a Panel called namePanel instead of a box to make sure it streches
        // according to the checkboxes when I resize the window
        JPanel namePanel = new JPanel(new GridLayout(NUM_INSTRUMENTS, 1));
        for (String instrumentName : instrumentNames) {
            JLabel instrumentLabel = new JLabel(instrumentName);
            instrumentLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
            namePanel.add(instrumentLabel);
        }

        background.add(BorderLayout.EAST, buttonBox);
        background.add(BorderLayout.WEST, namePanel);
        // place the left and right elements of out app, on the background panel

        frame.add(background);
        // add out "background" panel to the frame

        GridLayout grid = new GridLayout(NUM_INSTRUMENTS, NUM_BEATS);
        grid.setVgap(1);
        grid.setHgap(2);
        // Anohter layout manager, this one lets you put the components in a grid with rows and columns

        JPanel mainPanel = new JPanel(grid);
        background.add(BorderLayout.CENTER, mainPanel);
        // Creating a New Panel to be put above background panel. This will house the checkboxes

        checkboxList = new ArrayList<>();
        for (int i = 0; i < NUM_INSTRUMENTS*NUM_BEATS; i++) {
            JCheckBox c = new JCheckBox();
            c.setSelected(false);
            checkboxList.add(c);
            mainPanel.add(c);
        }

        frame.setBounds(50, 50, 300, 300);
        frame.pack();
        frame.setVisible(true);
    }

    private void setUpMidi() {
        // This is like setting up a DVD player, i just need to insert disc and play
        try {
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequence = new Sequence(Sequence.PPQ, 4);
            track = sequence.createTrack();
            sequencer.setTempoInBPM(120);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildTrackAndStart() {
        ArrayList<Integer> trackList;
        // We're making a 20 element array to hold the values for one instrument, 
        // across all 20 beats. If the instrument is supposed to play on that beat, the value 
        // at that element will be the key. If that element is not supposed to play, put a zero

        sequence.deleteTrack(track); // Get rid of old track, but why??
        track = sequence.createTrack(); // create a fresh one

        // There are 16 instruments, so do this for each of 16 rows(i.e, Bass, Congo)
        for (int i = 0; i < NUM_INSTRUMENTS; i++) { 
            trackList = new ArrayList<>(); 

            int key = instruments[i]; 
            // set the "key" that represents the instrument. Check instance variable instruments.

            for (int j = 0; j < NUM_BEATS; j++) { // do this for each of the beats for this row
                JCheckBox jc = checkboxList.get(j + NUM_BEATS *i);
                if (jc.isSelected()) {  // Is checkBox selected, if yes then put the
                    trackList.add(key); // key value in this slot in the array. This represents the beat
                } else {                // Otherwise instrument is not supposed to play at this moment.
                    trackList.add(null);// so set it to null.
                }
            }

            makeTracks(trackList);
            track.add(makeEvent(CONTROL_CHANGE, 1, 127, 0, NUM_BEATS));
        }
        track.add(makeEvent(PROGRAM_CHANGE, 9, 1, 0, NUM_BEATS-1));
        // We always want to make sure that there is an event at beat 20, there always is an
        // event at beat 20 (it goes 0 to 19). Other wise the BeatBox might not go the full 20
        // beats before  it starts over. This is kind of a dummy event to prevent early stop
        // when NOTE_ON, NOTE_OFF end much earlier liek in beat 10 or 12.

        try {
            sequencer.setSequence(sequence);
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            sequencer.setTempoInBPM(120);
            sequencer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void changeTempo(float tempoMultiplier) {
        float tempoFactor = sequencer.getTempoFactor();
        sequencer.setTempoFactor(tempoFactor * tempoMultiplier);
    }

    private void sendMessageAndTracks() {
        // This first part is similar to saveMuic(), but here we'll send checkboxState after making it
        boolean[] checkboxState = new boolean[NUM_BEATS*NUM_INSTRUMENTS];

        for (int i = 0; i < checkboxList.size() ; i++) {
            JCheckBox check = checkboxList.get(i);//check is an checkbox object. It has method getSelected()
            checkboxState[i] = check.isSelected();
        }
        // This is like SimpleChatClient, except instead of sending a string Message, we serailize
        // two objects  ( the String message and beat pattern) and write those 2  objects 
        // to the socket output stream (the server)
        try {
            out.writeObject(userName + nextNum++ + ": " + userMessage.getText());
            out.writeObject(checkboxState); 
            // If we ahd used PrintWriter, here we'd have written println(checkboxState)
        } catch (IOException e) {
            System.out.println("Terribly sorry. Could not send it to the server.");
            e.printStackTrace();
        }
        userMessage.setText(""); // Set the text area to be ""
    }
    
    public class RemoteReader implements Runnable {
        public void run() {
            try {
                Object obj;
                while ((obj = in.readObject()) != null) {
                    System.out.println("got an object from server");
                    System.out.println(obj.getClass());

                    String nameToShow = (String) obj;
                    boolean[] checkboxState = (boolean[]) in.readObject();
                    otherSeqsMap.put(nameToShow, checkboxState);
                    // otherSeqMap has the checkboxState associated with each person.

                    listVector.add(nameToShow);
                    incomingList.setListData(listVector);
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // This is an inner class This is also new - A ListSelectionListener that tells us 
    // when a user made a selection on the list of messages. When the user selects a message
    // we IMMEDIATELY laod the associated beat pattern(it's in the HashMap called otherSeqsMap)
    // and start playing it. There's some if tests because of little quirky things about 
    // getting ListSelectionEvents.
    public class MyListSelectionListener implements ListSelectionListener {
        public void valueChanged(ListSelectionEvent lse) {
            if (!lse.getValueIsAdjusting()) {
                String selected = incomingList.getSelectedValue();
                if(selected != null) {
                    // now go to the map and change its sequence
                    boolean[] selectedState = otherSeqsMap.get(selected);
                    changeSequence(selectedState);
                    sequencer.stop();
                    buildTrackAndStart();
                }
            }
        }
    }
 
    // This method is callled when the user selects something from the list. We IMMEDIATELY change
    // the pattern to the one they selected.
    private void changeSequence(boolean[] checkboxState) {
        for (int i = 0; i < NUM_INSTRUMENTS*NUM_BEATS; i++) {
            JCheckBox check = checkboxList.get(i);
            check.setSelected(checkboxState[i]);
        }
    }


    // This makes events for one instrument at a time, for all 16 beats.
    // So, it might get an int[] for the Bass drum, and each index in the array will 
    // hold either the key of that instrument or a zero. If it's a zero, the instrument
    // isn't supposed to play at that beat. Otherwise, make an event and add it to the track.
    private void makeTracks(ArrayList<Integer> list) {
        for (int i = 0; i < NUM_INSTRUMENTS; i++) {
            Integer instrumentKey = list.get(i);

            if (instrumentKey != null) {
                track.add(makeEvent(NOTE_ON, 9, instrumentKey, 100, i));
                track.add(makeEvent(NOTE_OFF, 9, instrumentKey, 100, i+1));
            }
        }        
    }

    public static MidiEvent makeEvent(int cmd, int chnl, int one, int two, int tick) {
        MidiEvent event = null;
        try {
            ShortMessage msg = new ShortMessage();
            msg.setMessage(cmd, chnl, one, two);
            event = new MidiEvent(msg, tick);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return event;
    }
}