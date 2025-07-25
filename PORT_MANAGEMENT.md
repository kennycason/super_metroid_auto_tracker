# 🎯 **Port Management Strategy**

## ❌ **What We Were Doing Wrong**
- Constantly changing ports: 8081 → 8082 → 8083 → ???
- Creating confusion between frontend and backend
- Accumulating zombie processes
- Poor development experience

## ✅ **Proper Approach**

### **Consistent Ports**
- **Python Server**: Port 8000 (original)
- **Kotlin Server**: Port 8081 (ALWAYS)
- **React App**: Port 3001 (Vite default)

### **Process Management**
```bash
# WRONG: Change port when conflict
./server.kexe 8082 1000  # Bad!

# RIGHT: Kill conflicts, same port
pkill -9 -f "server.kexe"
./server.kexe 8081 1000  # Good!
```

### **Automation Script**
Use `server/restart_server.sh`:
1. ✅ Kills existing processes
2. ✅ Checks port availability  
3. ✅ Builds if needed
4. ✅ Starts on consistent port 8081
5. ✅ Validates startup

## 🎮 **Current Working Setup**

| Service | Port | URL |
|---------|------|-----|
| **Python Server** | 8000 | http://localhost:8000/api/status |
| **Kotlin Server** | 8081 | http://localhost:8081/api/status |
| **React App** | 3001 | http://localhost:3001/ |

## 🚀 **Usage**

### Start Kotlin Server
```bash
cd server
./restart_server.sh
```

### Start React App  
```bash
cd super-metroid-tracker-react
npm run dev
```

### Switch Between Servers
Just update React's `BACKEND_URL`:
- Python: `'http://localhost:8000'`
- Kotlin: `'http://localhost:8081'`

**No more port hopping!** 🎯 