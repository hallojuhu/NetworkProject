package client;

import client.gui.GameField;
import registry.ClientObs;
import registry.ServerObs;
import server.field.Ship;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Arrays;

public class Client implements ClientObs
{
    private ServerObs server;
    private String regName;
    private boolean playerOnTurn;
    private GameField gameField;
    private final JFrame jFrame;
    private Ship[] ships;

    public Client(JFrame jFrame, String host, int port)
    {
        this.jFrame = jFrame;
        try {
            Registry registry = LocateRegistry.getRegistry( host, port);
            server = (ServerObs) registry.lookup("s");

            ClientObs clientObs = (ClientObs) UnicastRemoteObject.exportObject(this, 0);

            if (!Arrays.asList(registry.list()).contains("c1"))
            {
                regName = "c1";
                playerOnTurn = false;
            }
            else if (!Arrays.asList(registry.list()).contains("c2"))
            {
                regName = "c2";
                playerOnTurn = true;
            }
            else
            {
                System.out.println("server full");
                return;
            }
            System.out.println(server.addClient(regName, clientObs));

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public boolean placeShips(Point[] ships)
    {
        try
        {
            server.clientReady(regName);
            return server.placeShips(regName, ships);
        } catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     *
     * @param pos destination to shoot
     * @return if shot was hit; 0 = no hit; 1 = hit; 2 = hit + ship destroyed; 3 = hit + all ships destroyed;
     */
    public int shoot(int pos)
    {
        try
        {
            int hit = server.shoot(regName, pos);
            setPlayerOnTurn(hit > 0);
            return hit;
        } catch (RemoteException e)
        {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * @param pos pos of shot
     * @param onTurn: 0 = no hit; 1 = hit; 2 = hit + ship destroyed; 3 = hit + all ships destroyed;
     * @throws RemoteException
     */
    @Override
    public void shot(int pos, int onTurn) throws RemoteException
    {
        if (onTurn == 3)
            gameField.setGameEnd(false);
        setPlayerOnTurn(onTurn > 0);
        gameField.setGameComponentAsShot(0, pos);
    }

    @Override
    public void gameStart() throws RemoteException
    {
        this.gameField = new GameField(jFrame.getContentPane(),this);
        this.gameField.setupShips(ships);
        setPlayerOnTurn(playerOnTurn);
    }

    @Override
    public void messageReceived(String message) throws RemoteException
    {
        gameField.messageReceived(message);
    }

    public void sendMessage(String message)
    {
        try
        {
            server.messageReceived(regName, message);
        } catch (RemoteException e)
        {
            throw new RuntimeException(e);
        }
    }

    public boolean isPlayerOnTurn()
    {
        return playerOnTurn;
    }

    private void setPlayerOnTurn(boolean onTurn)
    {
        gameField.colorBorder(onTurn);
        playerOnTurn = onTurn;
    }

    public void setShips(Ship[] ships) {
        this.ships = ships;
    }
}
