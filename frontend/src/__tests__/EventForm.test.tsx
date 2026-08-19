import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, it, expect, vi, beforeEach } from "vitest";
import EventForm from "@/components/EventForm";

const mockCreateEvent = vi.fn();
const mockUpdateEvent = vi.fn();
const mockPublishEvent = vi.fn();
const mockCancelEvent = vi.fn();
const mockUploadBanner = vi.fn();
const mockUploadEventPhoto = vi.fn();
const mockAddEventVideo = vi.fn();
const mockDeleteEventMedia = vi.fn();
const mockReorderEventMedia = vi.fn();

vi.mock("@/lib/api", () => ({
  api: {
    createEvent: (...args: any[]) => mockCreateEvent(...args),
    updateEvent: (...args: any[]) => mockUpdateEvent(...args),
    publishEvent: (...args: any[]) => mockPublishEvent(...args),
    cancelEvent: (...args: any[]) => mockCancelEvent(...args),
    uploadBanner: (...args: any[]) => mockUploadBanner(...args),
    uploadEventPhoto: (...args: any[]) => mockUploadEventPhoto(...args),
    addEventVideo: (...args: any[]) => mockAddEventVideo(...args),
    deleteEventMedia: (...args: any[]) => mockDeleteEventMedia(...args),
    reorderEventMedia: (...args: any[]) => mockReorderEventMedia(...args),
  },
}));

const mockToastSuccess = vi.fn();
const mockToastError = vi.fn();
vi.mock("@/lib/toast", () => ({
  toast: {
    success: (...args: any[]) => mockToastSuccess(...args),
    error: (...args: any[]) => mockToastError(...args),
  },
}));

beforeEach(() => {
  mockCreateEvent.mockReset();
  mockUpdateEvent.mockReset();
  mockPublishEvent.mockReset();
  mockCancelEvent.mockReset();
  mockUploadBanner.mockReset();
  mockUploadEventPhoto.mockReset();
  mockAddEventVideo.mockReset();
  mockDeleteEventMedia.mockReset();
  mockReorderEventMedia.mockReset();
  mockToastSuccess.mockReset();
  mockToastError.mockReset();
  global.URL.createObjectURL = vi.fn(() => "blob:test-preview");
});

function getBannerFileInput(container: HTMLElement) {
  const bannerHeading = screen.getByRole("heading", { name: "Banner" });
  return bannerHeading.closest(".card-md")!.querySelector('input[type="file"]') as HTMLInputElement;
}

function getTitleInput() {
  return document.querySelector('input.input[value=""]') as HTMLInputElement;
}

function getDescriptionInput() {
  return document.querySelector('textarea.input') as HTMLTextAreaElement;
}

function fillRequiredFields() {
  const inputs = document.querySelectorAll('input.input[type="date"], input.input[value=""]');
  const dateInput = document.querySelector('input[type="date"]') as HTMLInputElement;
  const deadlineInput = document.querySelector('input[type="datetime-local"]') as HTMLInputElement;
  const titleInput = screen.getAllByDisplayValue("").find((el) => el.tagName === "INPUT" && !(el as HTMLInputElement).type) as HTMLInputElement;

  if (titleInput) fireEvent.change(titleInput, { target: { value: "Test Event" } });
  if (dateInput) fireEvent.change(dateInput, { target: { value: "2028-01-15" } });
  if (deadlineInput) fireEvent.change(deadlineInput, { target: { value: "2028-01-10T18:00" } });
}

describe("EventForm — render", () => {
  it("renders all form sections", () => {
    render(<EventForm />);
    expect(screen.getByRole("heading", { name: "Basic details" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Banner" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Schedule" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Venue" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Capacity & pricing" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Important notes" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Contact" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Media gallery" })).toBeInTheDocument();
  });

  it("shows dual-tier pricing sections", () => {
    render(<EventForm />);
    expect(screen.getByText("Member tickets")).toBeInTheDocument();
    expect(screen.getByText("Guest tickets")).toBeInTheDocument();
    expect(screen.getByText("Set Max to 0 to disallow guest tickets.")).toBeInTheDocument();
  });

  it("shows 'Create Draft' button when no eventId", () => {
    render(<EventForm />);
    expect(screen.getByText("Create Draft")).toBeInTheDocument();
  });

  it("shows action buttons when eventId is provided", () => {
    render(<EventForm eventId="evt-123" />);
    expect(screen.getByText("Save Changes")).toBeInTheDocument();
    expect(screen.getByText("Publish")).toBeInTheDocument();
    expect(screen.getByText("Cancel Event")).toBeInTheDocument();
  });

  it("shows 'Save Draft' when eventStatus is DRAFT", () => {
    render(<EventForm eventId="evt-123" eventStatus="DRAFT" />);
    expect(screen.getByText("Save Draft")).toBeInTheDocument();
  });

  it("hides media section when no eventId", () => {
    render(<EventForm />);
    expect(screen.getByText("Save the event before adding media.")).toBeInTheDocument();
  });

  it("shows media section when eventId is provided", () => {
    render(<EventForm eventId="evt-123" />);
    expect(screen.getByText("Upload photo")).toBeInTheDocument();
    expect(screen.getAllByText("Add video").length).toBeGreaterThanOrEqual(1);
  });
});

describe("EventForm — banner upload", () => {
  it("shows upload placeholder when no banner", () => {
    render(<EventForm />);
    expect(screen.getByText("Click to upload a banner image")).toBeInTheDocument();
  });

  it("shows preview when bannerUrl is provided via initial", () => {
    render(<EventForm initial={{ bannerUrl: "https://example.com/banner.jpg" }} />);
    expect(screen.getByRole("img")).toHaveAttribute("src", "https://example.com/banner.jpg");
    expect(screen.getByText("Change banner")).toBeInTheDocument();
  });

  it("uploads banner immediately when eventId exists", async () => {
    mockUploadBanner.mockResolvedValueOnce({ bannerUrl: "https://s3.example.com/new-banner.png" });

    const { container } = render(<EventForm eventId="evt-123" />);
    const fileInput = getBannerFileInput(container);
    await userEvent.setup().upload(fileInput, new File(["content"], "test.png", { type: "image/png" }));

    await waitFor(() => {
      expect(mockUploadBanner).toHaveBeenCalledWith("evt-123", expect.any(File));
    });
  });

  it("shows blob preview when banner selected before event exists", async () => {
    const { container } = render(<EventForm />);
    const fileInput = getBannerFileInput(container);
    await userEvent.setup().upload(fileInput, new File(["content"], "banner.png", { type: "image/png" }));

    expect(screen.getByRole("img")).toHaveAttribute("src", "blob:test-preview");
    expect(mockUploadBanner).not.toHaveBeenCalled();
  });

  it("uploads pending banner after create draft", async () => {
    mockCreateEvent.mockResolvedValueOnce({ id: "evt-new-1", bannerUrl: null });
    mockUploadBanner.mockResolvedValueOnce({ bannerUrl: "https://s3.example.com/uploaded.png" });

    const { container } = render(<EventForm />);
    const fileInput = getBannerFileInput(container);
    await userEvent.setup().upload(fileInput, new File(["data"], "banner.png", { type: "image/png" }));

    fillRequiredFields();
    fireEvent.click(screen.getByText("Create Draft"));

    await waitFor(() => {
      expect(mockCreateEvent).toHaveBeenCalledTimes(1);
    });
    await waitFor(() => {
      expect(mockUploadBanner).toHaveBeenCalledWith("evt-new-1", expect.any(File));
    });
  });
});

describe("EventForm — create draft", () => {
  it("calls createEvent with form values and calls onSaved", async () => {
    const onSaved = vi.fn();
    mockCreateEvent.mockResolvedValueOnce({ id: "evt-456", bannerUrl: null });

    render(<EventForm onSaved={onSaved} />);

    fillRequiredFields();
    fireEvent.click(screen.getByText("Create Draft"));

    await waitFor(() => {
      expect(mockCreateEvent).toHaveBeenCalledTimes(1);
    });
    expect(onSaved).toHaveBeenCalledWith({ id: "evt-456", bannerUrl: null });
  });

  it("disables button while submitting", async () => {
    let resolve: (v: any) => void;
    mockCreateEvent.mockReturnValueOnce(new Promise((r) => { resolve = r; }));

    render(<EventForm />);
    fillRequiredFields();
    fireEvent.click(screen.getByText("Create Draft"));

    expect(screen.getByText("Creating…")).toBeInTheDocument();

    resolve!({ id: "evt-1", bannerUrl: null });
    await waitFor(() => {
      expect(screen.getByText("Save Changes")).toBeInTheDocument();
    });
  });
});

describe("EventForm — important notes", () => {
  it("adds and removes notes", async () => {
    render(<EventForm />);

    const noteInput = screen.getByPlaceholderText("Add a note…");
    await userEvent.setup().type(noteInput, "First note");
    fireEvent.click(screen.getByText("Add"));
    expect(screen.getByText("• First note")).toBeInTheDocument();

    await userEvent.setup().type(noteInput, "Second note");
    fireEvent.click(screen.getByText("Add"));
    expect(screen.getByText("• Second note")).toBeInTheDocument();

    fireEvent.click(screen.getAllByText("Remove")[0]);
    expect(screen.queryByText("• First note")).not.toBeInTheDocument();
    expect(screen.getByText("• Second note")).toBeInTheDocument();
  });
});

describe("EventForm — save draft & publish", () => {
  it("calls updateEvent on save", async () => {
    mockUpdateEvent.mockResolvedValueOnce({ id: "evt-1" });

    render(<EventForm eventId="evt-1" initial={{ title: "Existing" }} />);
    fireEvent.click(screen.getByText("Save Changes"));

    await waitFor(() => {
      expect(mockUpdateEvent).toHaveBeenCalledWith("evt-1", expect.objectContaining({ title: "Existing" }));
    });
  });

  it("calls publishEvent on publish", async () => {
    mockPublishEvent.mockResolvedValueOnce({ id: "evt-1" });

    render(<EventForm eventId="evt-1" />);
    fireEvent.click(screen.getByText("Publish"));

    await waitFor(() => {
      expect(mockPublishEvent).toHaveBeenCalledWith("evt-1");
    });
  });
});
