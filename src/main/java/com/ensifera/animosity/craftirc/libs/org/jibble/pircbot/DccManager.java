/* 
Copyright Paul James Mutton, 2001-2009, http://www.jibble.org/

This file is part of PircBot.

This software is dual-licensed, allowing you to choose between the GNU
General Public License (GPL) and the www.jibble.org Commercial License.
Since the GPL may be too restrictive for use in a proprietary application,
a commercial license is also provided. Full license information can be
found at http://www.jibble.org/licenses/

*/

package com.ensifera.animosity.craftirc.libs.org.jibble.pircbot;

import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class is used to process DCC events from the server.
 *
 * @author PircBot-PPF project
 * @version 1.0.0
 */
public class DccManager {

    /**
     * Constructs a DccManager to look after all DCC SEND and CHAT events.
     *
     * @param bot The PircBot whose DCC events this class will handle.
     */
    DccManager(PircBot bot) {
        _bot = bot;
    }

    /**
     * Processes a DCC request.
     *
     * @return True if the type of request was handled successfully.
     */
    boolean processRequest(String nick, String login, String hostname, String request) {
        StringTokenizer tokenizer = new StringTokenizer(request);
        tokenizer.nextToken();
        String type = tokenizer.nextToken();
        String filename = tokenizer.nextToken();

        switch (type) {
            case "SEND": {
                long address = Long.parseLong(tokenizer.nextToken());
                int port = Integer.parseInt(tokenizer.nextToken());
                long size = -1;
                try {
                    size = Long.parseLong(tokenizer.nextToken());
                } catch (Exception e) {
                    // Stick with the old value.
                }

                DccFileTransfer transfer = new DccFileTransfer(_bot, this, nick, login, hostname, type, filename, address, port, size);
                _bot.onIncomingFileTransfer(transfer);

                break;
            }
            case "RESUME": {
                int port = Integer.parseInt(tokenizer.nextToken());
                long progress = Long.parseLong(tokenizer.nextToken());

                DccFileTransfer transfer = null;
                synchronized (_awaitingResume) {
                    for (int i = 0; i < _awaitingResume.size(); i++) {
                        transfer = _awaitingResume.elementAt(i);
                        if (transfer.getNick().equals(nick) && transfer.getPort() == port) {
                            _awaitingResume.removeElementAt(i);
                            break;
                        }
                    }
                }

                if (transfer != null) {
                    transfer.setProgress(progress);
                    _bot.sendCTCPCommand(nick, "DCC ACCEPT file.ext " + port + " " + progress);
                }

                break;
            }
            case "ACCEPT": {
                int port = Integer.parseInt(tokenizer.nextToken());

                DccFileTransfer transfer = null;
                synchronized (_awaitingResume) {
                    for (int i = 0; i < _awaitingResume.size(); i++) {
                        transfer = _awaitingResume.elementAt(i);
                        if (transfer.getNick().equals(nick) && transfer.getPort() == port) {
                            _awaitingResume.removeElementAt(i);
                            break;
                        }
                    }
                }

                if (transfer != null) {
                    transfer.doReceive(transfer.getFile(), true);
                }

                break;
            }
            case "CHAT": {
                long address = Long.parseLong(tokenizer.nextToken());
                int port = Integer.parseInt(tokenizer.nextToken());

                final DccChat chat = new DccChat(_bot, nick, login, hostname, address, port);

                new Thread() {
                    public void run() {
                        _bot.onIncomingChatRequest(chat);
                    }
                }.start();
                break;
            }
            default:
                return false;
        }

        return true;
    }

    /**
     * Add this DccFileTransfer to the list of those awaiting possible
     * resuming.
     *
     * @param transfer the DccFileTransfer that may be resumed.
     */
    void addAwaitingResume(DccFileTransfer transfer) {
        synchronized (_awaitingResume) {
            _awaitingResume.addElement(transfer);
        }
    }

    /**
     * Remove this transfer from the list of those awaiting resuming.
     */
    void removeAwaitingResume(DccFileTransfer transfer) {
        _awaitingResume.removeElement(transfer);
    }

    private final PircBot _bot;
    private final Vector<DccFileTransfer> _awaitingResume = new Vector<>();

}
