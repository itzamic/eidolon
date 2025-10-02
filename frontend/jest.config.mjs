// Jest configuration for Next.js (ESM)
import nextJest from 'next/jest.js';

const createJestConfig = nextJest({
  dir: './',
});

const config = {
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',
  },
  testMatch: ['**/__tests__/**/*.(spec|test).(ts|tsx)', '**/?(*.)+(spec|test).(ts|tsx)'],
};

export default createJestConfig(config);
