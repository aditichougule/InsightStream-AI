# AI Video Intelligence Platform - Frontend

Modern Next.js frontend for the AI Video Intelligence Platform with beautiful UI components, real-time updates, and seamless integration with the Spring Boot backend.

## Quick Start

### Prerequisites
- Node.js 18+ (installed via Homebrew)
- npm or yarn

### Installation

1. **Navigate to frontend directory:**
```bash
cd frontend
```

2. **Install dependencies:**
```bash
npm install
```

3. **Set up environment variables:**
```bash
cp .env.local.example .env.local
```

Edit `.env.local` and update the backend API URL if needed:
```
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

4. **Start the development server:**
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

## Project Structure

```
frontend/
├── app/                          # Next.js App Router
│   ├── layout.tsx               # Root layout
│   ├── page.tsx                 # Home page
│   ├── globals.css              # Global styles
│   └── (auth)/                  # Authentication routes
├── components/                   # Reusable components
│   ├── ui/                      # shadcn/ui components
│   ├── common/                  # Common components (Header, Footer, etc.)
│   ├── forms/                   # Form components
│   └── videos/                  # Video-related components
├── lib/                         # Utility functions
│   ├── utils.ts                # Tailwind utilities
│   ├── axios.ts                # Axios instance setup
│   └── api/                    # API client functions
├── public/                      # Static assets
├── package.json                 # Dependencies
├── tsconfig.json               # TypeScript config
├── tailwind.config.ts          # Tailwind CSS config
├── next.config.ts              # Next.js config
└── components.json             # shadcn/ui config
```

## Tech Stack

### Core
- **Framework:** Next.js 16.2 with App Router
- **Language:** TypeScript 5
- **Styling:** Tailwind CSS v4
- **UI Components:** shadcn/ui (based on Radix UI)
- **HTTP Client:** Axios 1.16
- **Icons:** Lucide React

### Development
- **Linting:** ESLint 9
- **Type Checking:** TypeScript 5
- **Build Tool:** Next.js bundler

## Available Scripts

```bash
# Development server (hot reload)
npm run dev

# Production build
npm run build

# Start production server
npm start

# Run linting
npm run lint
```

## Configuration

### Environment Variables

Create a `.env.local` file in the frontend directory:

```env
# Backend API
NEXT_PUBLIC_API_URL=http://localhost:8080/api

# Token storage strategy
NEXT_PUBLIC_TOKEN_STORAGE=localStorage

# API timeout
NEXT_PUBLIC_API_TIMEOUT=30000

# App details
NEXT_PUBLIC_APP_NAME=AI Video Intelligence Platform
NEXT_PUBLIC_APP_VERSION=0.1.0
```

### Tailwind CSS

The project uses Tailwind CSS v4 with custom configuration in `tailwind.config.ts`. Extend it with your custom colors and utilities as needed.

### shadcn/ui Components

Components are installed in `components/ui/`. Add more components as needed:

```bash
npx shadcn@latest add <component-name>
```

Popular components for this project:
- `button` - Button component (already included)
- `card` - Card container
- `input` - Text input
- `form` - Form wrapper with react-hook-form integration
- `dialog` - Modal dialog
- `dropdown-menu` - Dropdown menu
- `tabs` - Tab navigation
- `pagination` - Pagination controls
- `skeleton` - Loading skeleton
- `alert` - Alert messages
- `badge` - Badge/tag component

## API Integration

### Axios Setup

The project uses Axios for API calls. Create `lib/axios.ts`:

```typescript
import axios from 'axios';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  timeout: parseInt(process.env.NEXT_PUBLIC_API_TIMEOUT || '30000'),
});

// Add JWT token to requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('authToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default api;
```

### API Client Functions

Create `lib/api/` directory with service files for each API resource:

```typescript
// lib/api/videos.ts
import api from '@/lib/axios';

export const videosApi = {
  create: (data: CreateVideoRequest) => api.post('/videos', data),
  getById: (id: number) => api.get(`/videos/${id}`),
  list: (userId: number, page = 0, size = 10) =>
    api.get(`/videos/user/${userId}?page=${page}&size=${size}`),
  update: (id: number, data: CreateVideoRequest) =>
    api.put(`/videos/${id}`, data),
  delete: (id: number) => api.delete(`/videos/${id}`),
};
```

## Backend Integration

This frontend connects to the Spring Boot backend at `http://localhost:8080`.

### Ensure Backend is Running

```bash
cd .. # Go back to root directory
mvn spring-boot:run # or with profile: -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

The API should be available at:
- **Health Check:** `http://localhost:8080/health`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **API Base URL:** `http://localhost:8080/api`

## Development Workflow

### 1. Create a New Page

```typescript
// app/videos/page.tsx
'use client';

import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';

export default function VideosPage() {
  return (
    <div>
      <h1>Videos</h1>
      <Button>New Video</Button>
    </div>
  );
}
```

### 2. Create a Reusable Component

```typescript
// components/videos/VideoCard.tsx
import { Card } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

interface VideoCardProps {
  id: number;
  title: string;
  description: string;
  onView?: () => void;
}

export function VideoCard({ id, title, description, onView }: VideoCardProps) {
  return (
    <Card className="p-4">
      <h3 className="text-lg font-semibold">{title}</h3>
      <p className="text-sm text-gray-600">{description}</p>
      <Button onClick={onView} className="mt-4">View</Button>
    </Card>
  );
}
```

### 3. Add Form with Validation

Install react-hook-form:
```bash
npm install react-hook-form
```

```typescript
// components/forms/CreateVideoForm.tsx
'use client';

import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { videosApi } from '@/lib/api/videos';

export function CreateVideoForm() {
  const { register, handleSubmit, formState: { errors } } = useForm();

  const onSubmit = async (data: any) => {
    try {
      const response = await videosApi.create(data);
      console.log('Video created:', response.data);
    } catch (error) {
      console.error('Failed to create video:', error);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Input {...register('title', { required: true })} placeholder="Title" />
      {errors.title && <span>Title is required</span>}
      <Button type="submit">Create</Button>
    </form>
  );
}
```

## Performance Optimization

### Image Optimization
Use Next.js Image component for automatic optimization:

```typescript
import Image from 'next/image';

<Image
  src="/video-placeholder.jpg"
  alt="Video"
  width={400}
  height={300}
/>
```

### Code Splitting
Components are automatically code-split by Next.js. Use dynamic imports for large components:

```typescript
import dynamic from 'next/dynamic';

const HeavyComponent = dynamic(() => import('@/components/Heavy'), {
  loading: () => <Skeleton />,
});
```

### Caching
Configure Next.js caching strategies in `next.config.ts`:

```typescript
const nextConfig: NextConfig = {
  headers: async () => {
    return [
      {
        source: '/api/:path*',
        headers: [
          {
            key: 'Cache-Control',
            value: 'no-cache, no-store, must-revalidate',
          },
        ],
      },
    ];
  },
};
```

## Building for Production

```bash
# Build the application
npm run build

# Start production server
npm start

# Build stats
npm run build -- --analyze
```

The optimized build will be in the `.next` directory.

## Deployment Options

### Vercel (Recommended)
The project is optimized for deployment on Vercel:

```bash
npm install -g vercel
vercel
```

### Docker
Create a `Dockerfile` in the frontend directory:

```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:18-alpine AS runner
WORKDIR /app
ENV NODE_ENV production
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
EXPOSE 3000
CMD ["npm", "start"]
```

Build: `docker build -t ai-video-platform-frontend .`
Run: `docker run -p 3000:3000 ai-video-platform-frontend`

### Traditional Server (Node.js)
1. Build: `npm run build`
2. Deploy `.next`, `package.json`, and `node_modules`
3. Run: `npm start`

## Troubleshooting

### Port 3000 Already in Use
```bash
# macOS/Linux
lsof -i :3000
kill -9 <PID>

# Windows
netstat -ano | findstr :3000
taskkill /PID <PID> /F
```

### Backend Connection Issues
- Ensure Spring Boot backend is running: `http://localhost:8080/health`
- Check `NEXT_PUBLIC_API_URL` in `.env.local`
- Check CORS configuration in backend

### Module Not Found Errors
```bash
rm -rf node_modules package-lock.json
npm install
```

## Testing

Add testing later (Jest + React Testing Library):

```bash
npm install --save-dev jest @testing-library/react @testing-library/jest-dom
```

## Contributing

Follow the project structure and coding standards:
- Use TypeScript for type safety
- Component files: PascalCase (`VideoCard.tsx`)
- Utility files: camelCase (`apiClient.ts`)
- Add JSDoc comments for complex functions
- Keep components small and reusable

## Next Steps

1. ✅ Frontend setup complete
2. Create authentication pages (login, register)
3. Create video management pages
4. Add real-time features (WebSocket)
5. Implement video player with timestamps
6. Add search and filtering UI

## Resources

- [Next.js Documentation](https://nextjs.org/docs)
- [Tailwind CSS](https://tailwindcss.com)
- [shadcn/ui](https://ui.shadcn.com)
- [Axios Documentation](https://axios-http.com)
- [TypeScript](https://www.typescriptlang.org)

## License

Same as the main project.
