const express = require('express');
const bodyParser = require('body-parser');
const jwt = require('jsonwebtoken');
const cors = require('cors');
const http = require('http');
const { Server } = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: {
        origin: "*",
        methods: ["GET", "POST"]
    }
});
const PORT = 3000;
const SECRET_KEY = 'my_secret_key';

app.use(cors());
app.use(bodyParser.json());

// In-memory data
const users = [
    { username: 'a', password: 'a' },
    { username: 'b', password: 'b' }
];

let items = [
    { id: 1, name: 'Task 1', description: 'Description 1', date: '2023-11-20', value: 10, flag: true },
    { id: 2, name: 'Task 2', description: 'Description 2', date: '2023-11-21', value: 20, flag: false },
    { id: 3, name: 'Task 3', description: 'Description 3', date: '2023-11-22', value: 30, flag: true }
];

let nextId = 4;

const authenticateJWT = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (authHeader) {
        const token = authHeader.split(' ')[1];
        jwt.verify(token, SECRET_KEY, (err, user) => {
            if (err) {
                return res.sendStatus(403);
            }
            req.user = user;
            next();
        });
    } else {
        res.sendStatus(401);
    }
};

// Login Endpoint
app.post('/auth/login', (req, res) => {
    const { username, password } = req.body;
    const user = users.find(u => u.username === username && u.password === password);

    if (user) {
        const token = jwt.sign({ username: user.username }, SECRET_KEY, { expiresIn: '1h' });
        res.json({ token });
    } else {
        res.status(401).json({ message: 'Invalid credentials' });
    }
});

// Logout Endpoint
app.post('/auth/logout', authenticateJWT, (req, res) => {
    // In a real app, you might blacklist the token here
    res.json({ message: 'Logged out successfully' });
});

// Item Endpoints
app.get('/item', authenticateJWT, (req, res) => {
    res.json(items);
});

app.post('/item', authenticateJWT, (req, res) => {
    const newItem = req.body;
    newItem.id = nextId++;
    items.push(newItem);
    console.log('Items after adding:', items);

    // Broadcast to all connected clients
    io.emit('itemAdded', newItem);

    res.json(newItem);
});

app.put('/item/:id', authenticateJWT, (req, res) => {
    const id = parseInt(req.params.id);
    const updatedItem = req.body;
    const index = items.findIndex(i => i.id === id);

    if (index !== -1) {
        updatedItem.id = id;
        items[index] = updatedItem;
        console.log('Items after updating:', items);

        // Broadcast to all connected clients
        io.emit('itemUpdated', updatedItem);

        res.json(updatedItem);
    } else {
        res.status(404).json({ message: 'Item not found' });
    }
});

app.delete('/item/:id', authenticateJWT, (req, res) => {
    const id = parseInt(req.params.id);
    const index = items.findIndex(i => i.id === id);

    if (index !== -1) {
        const deletedItem = items[index];
        items.splice(index, 1);
        console.log('Items after deleting:', items);

        // Broadcast to all connected clients
        io.emit('itemDeleted', deletedItem);

        res.json(deletedItem);
    } else {
        res.status(404).json({ message: 'Item not found' });
    }
});

// WebSocket connection handling
io.on('connection', (socket) => {
    console.log('Client connected:', socket.id);

    socket.on('disconnect', () => {
        console.log('Client disconnected:', socket.id);
    });
});

server.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
