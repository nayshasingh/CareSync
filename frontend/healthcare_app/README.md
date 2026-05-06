# Healthcare Frontend

Angular frontend for the Smart Healthcare Appointment System.

## Development server

Install dependencies and start the local frontend:

```bash
npm install
npm start
```

Once the server is running, open `http://localhost:4200/`.

The frontend expects the Spring Boot backend to be available at `http://localhost:8080`.

## Available Scripts

```bash
npm start      # Run Angular dev server
npm run build  # Create production build
npm test       # Run frontend tests
```

## Backend Integration

From this frontend directory, start the backend with Docker Compose:

```bash
cp ../../.env.example ../../.env
docker compose -f ../../docker-compose.yml up --build
```

Useful local URLs:

- Backend API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Mailpit Inbox: `http://localhost:8025`

## Notes

- This project uses Angular 19 and Angular Material.
- Backend API URLs are configured in the frontend environment files.
