import { render, screen } from '@testing-library/react';
import HeapChart from '@/components/HeapChart';

describe('HeapChart', () => {
  it('renders the chart with aria label and used legend', () => {
    const points = [
      { t: 0, used: 0, max: null },
      { t: 1, used: 2048, max: null },
    ];
    render(<HeapChart points={points} height={200} />);
    expect(screen.getByRole('img', { name: /heap usage chart/i })).toBeInTheDocument();
    expect(screen.getByText('Used: 2.0 KB')).toBeInTheDocument();
  });

  it('renders max legend when maxBytesHint is provided', () => {
    const points = [
      { t: 0, used: 512, max: 1048576 },
      { t: 1, used: 1024, max: 1048576 },
    ];
    render(<HeapChart points={points} maxBytesHint={1048576} height={200} />);
    expect(screen.getByText('Used: 1.0 KB')).toBeInTheDocument();
    expect(screen.getByText('Max: 1.0 MB')).toBeInTheDocument();
  });
});
