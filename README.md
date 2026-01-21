# Load Balancer + Proxy Flow

## Connection Flow

```text
        Player
           |
           | Initial connection
           v
+------------------------+
|   Load Balancer (LB)   |
| - No player hosting   |
| - No proxying         |
+------------------------+
           |
           | 1.20.4 Transfer Packet
           v
+------------------------+
|   Proxy (Lowest Load) |
+------------------------+
           |
           | Normal gameplay
           v
     Backend Servers
```

## Proxy Failure Flow

```text
      Proxy crashes
           |
           v
  Only players on that proxy disconnect
           |
           v
        Reconnect
           |
           v
+------------------------+
|   Load Balancer (LB)   |
+------------------------+
           |
           v
+------------------------+
|  Another healthy proxy|
+------------------------+
```

## Explanation

- Players only touch the **Load Balancer (LB)** for a moment
- The LB checks which proxy has the lowest player count
- The player is transferred using the **1.20.4 transfer packet**
- After transfer, the LB is completely out of the connection
- Proxies hold the actual long-lived connections

## Why This Is Reliable

- LB does not proxy traffic
- LB does not host players
- No active connections are kept on the LB
- If a proxy goes down, only its players disconnect
- Reconnecting players are sent to a healthy proxy

## Summary

```text
Load Balancer = redirect only
Proxy         = real connection
```
# Minecraft-Load-Balance
