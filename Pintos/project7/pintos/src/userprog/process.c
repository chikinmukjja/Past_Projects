#include "userprog/process.h"
#include <debug.h>
#include <inttypes.h>
#include <round.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "userprog/gdt.h"
#include "userprog/pagedir.h"
#include "userprog/tss.h"
#include "filesys/directory.h"
#include "filesys/file.h"
#include "filesys/filesys.h"
#include "threads/flags.h"
#include "threads/init.h"
#include "threads/interrupt.h"
#include "threads/palloc.h"
#include "threads/thread.h"
#include "threads/vaddr.h"
#include "vm/page.h"
#include "vm/frame.h"
#include "vm/swap.h"
#include "threads/malloc.h"
#include "userprog/syscall.h"

#define MAX_STACK_SIZE (1 << 23)

static bool install_page(void *upage, void *kpage, bool writable);

bool expand_stack(void *addr);
bool handle_mm_fault(struct vm_entry *vme);

static thread_func start_process NO_RETURN;
static bool load(const char *cmdline, void (**eip)(void), void **esp);
void Argument_Stack(char **parse, int count, void **esp);

struct thread *get_child_process(int pid);
void remove_child_process(struct thread *cp);

int process_add_file(struct file *f);
struct file *process_get_file(int fd);
int process_add_file(struct file *f);
void process_close_file(int fd);

bool expand_stack(void *addr)
{

	bool success = false;
	uint8_t *kpage;

	if(pg_round_down(addr) >= PHYS_BASE - MAX_STACK_SIZE) // check over stack maxsize (8Mbyte)
	{
		struct page *page = alloc_page(PAL_USER);  //allocate page
		if(page == NULL)return false;

		kpage = page->kaddr;

		struct vm_entry* vme = malloc(sizeof(struct vm_entry));
		if(vme == NULL) // handle exception
		{
			free_page(kpage);
			return false;
		}
		vme->type = VM_ANON;      // initial vm_entry
		vme->vaddr = pg_round_down(addr);
		vme->is_loaded = true;
		vme->writable = true;
		vme->pinned = true;      // stack is not victim for swap

		success = install_page(vme->vaddr,kpage,true);
		if(!success)
		{
			free_page(kpage);
			return false;
		}
		page->vme = vme;

		if(!insert_vme(&thread_current()->vm,vme)) // insert vm_entry list
		{
			free(vme);
			free_page(kpage);
			success = false;
		}

	}
	else {exit(-1);}                 // handle exception for segmentation fault
	return success;

}

bool handle_mm_fault(struct vm_entry *vme) {
	uint8_t *kpage;
	struct page *page;
//	kpage = palloc_get_page(PAL_USER | PAL_ZERO); //get page in user pool

	bool success = false;                           //return value

	if (vme->is_loaded)             // already loaded -> not allocate
	{
		return true;
	}
	page = alloc_page(PAL_USER);
	page->vme = vme;
	kpage = page->kaddr;

	switch (vme->type)               // type of segment
	{
	case VM_BIN:                //text, code
		success = load_file(kpage, vme);
		break;
	case VM_FILE:               // mapping file
		success = load_file(kpage, vme);
		break;
	case VM_ANON:               // another like stack
		swap_in(vme->swap_slot,page->kaddr);
		success = true;
	default:
		break;


	}


	if (!install_page(vme->vaddr, kpage, vme->writable))
	{
		free_page(kpage);
		return false;
	}
	vme->is_loaded = true;


		   // load failed -> free




	return success;
}

/* Starts a new thread running a user program loaded from
 FILENAME.  The new thread may be scheduled (and may even exit)
 before process_execute() returns.  Returns the new process's
 thread id, or TID_ERROR if the thread cannot be created. */
tid_t process_execute(const char *file_name) {
	char *fn_copy;
	tid_t tid;
	char *tmp_file_name;

	/* Make a copy of FILE_NAME.
	 Otherwise there's a race between the caller and load(). */
	fn_copy = palloc_get_page(0);
	if (fn_copy == NULL)
		return TID_ERROR;
	strlcpy(fn_copy, file_name, PGSIZE);

	tmp_file_name = palloc_get_page(0);
	if (tmp_file_name == NULL)
		return TID_ERROR;
	strlcpy(tmp_file_name, file_name, PGSIZE);

	//=============== Mycode begin ==========================================

	char *next_pointer;          //pointer : after parsing, point next string
	tmp_file_name = strtok_r(tmp_file_name, " ", &next_pointer); // file_name <= token

	//===============  Mycode end  ==========================================

	/* Create a new thread to execute FILE_NAME. */
	tid = thread_create(tmp_file_name, PRI_DEFAULT, start_process, fn_copy);

	if (tid == TID_ERROR)
		palloc_free_page(fn_copy);
	palloc_free_page(tmp_file_name);
	return tid;
}

/* A thread function that loads a user process and starts it
 running. */
static void start_process(void *file_name_) {
	char *file_name = file_name_;
	struct intr_frame if_;
	bool success;

//=========================Mycode begin==================================
	int count = 0;                        // count the number of argument
	char* next_pointer;                  // point next location of string
	char* token;                         // token parsed
	char* parsed_argument[LOADER_ARGS_LEN / 2 + 1];

	vm_init(&thread_current()->vm);
	// why?
	/*  LOADER_ARGS_LEN is defined 128                                */
	/*  In init.c, function read_command_line argv declared :         */
	/*  static char * [LOADER_ARGS_LEN/2+1]; so limit parsed_argument */

//========================= Mycode end =================================
	/* Initialize interrupt frame and load executable. */
	memset(&if_, 0, sizeof if_);
	if_.gs = if_.fs = if_.es = if_.ds = if_.ss = SEL_UDSEG;
	if_.cs = SEL_UCSEG;
	if_.eflags = FLAG_IF | FLAG_MBS;

//========================MYcode begin=========================================

	for (token = strtok_r(file_name, " ", &next_pointer); // parsing conmand line
			token != NULL;                                 // to store on stack
			token = strtok_r(NULL, " ", &next_pointer)) {
		parsed_argument[count++] = token;               // store parsed argument
//    printf("'%s'\n",token);
	}

	success = load(file_name, &if_.eip, &if_.esp);    // make virtual memory

	thread_current()->process_load = success;    // success ture -> load success

//if (success)
//			Argument_Stack(parsed_argument, count, &if_.esp);
	int i;
	for( i=0;i<1000;i++);
	sema_up(&thread_current()->load_semaphore);        // wake of parent

//	palloc_free_page(file_name);
//  hex_dump(if_.esp , if_.esp , PHYS_BASE - if_.esp , true); // to see result 

//====================== Mycode end ===========================================

	/* If load failed, quit. */
	if (success)
			Argument_Stack(parsed_argument, count, &if_.esp); // function make userstack



	//palloc_free_page(file_name);
     // *******************  above critical syn write and read *************************************//


	if (!success) {                                   // failed load
		thread_current()->exit_status = -1;
		thread_exit();
	}

	/* Start the user process by simulating a return from an
	 interrupt, implemented by intr_exit (in
	 threads/intr-stubs.S).  Because intr_exit takes all of its
	 arguments on the stack in the form of a `struct intr_frame',
	 we just point the stack pointer (%esp) to our stack frame
	 and jump to it. */

	asm volatile ("movl %0, %%esp; jmp intr_exit" : : "g" (&if_) : "memory");
	NOT_REACHED ();
}
void Argument_Stack(char **parse, int count, void **esp) {
	int i, j, k = 0;                                      // just for loop count
	uint32_t argv_address[count];                  // variable :
												   //     store address of argv

	for (i = count - 1; i > -1; i--)                 // make userstack
			{
		for (k += j = strlen(parse[i]); j > -1; j--)      // decreasing esp,
				{                                      // store data of argument
			*esp = *esp - 1;
			**(char **) esp = parse[i][j];
		}

		argv_address[i] = *esp;                  // and for argument,
	}                                              // storing address of it

												   // k = (k+count)%4;                               // to find how many we need
												   // word-align

	// for(i = 0; 4 - k >i ; i++)
	// {
	*esp = *esp - 1;                          // push word-align
	*(uint8_t*) *esp = 0;
	// }

	*esp = *esp - 4;                              // push list address of argv
	*(uint32_t*) *esp = 0;

	for (i = count - 1; i >= 0; i--)                 // making stack of
			{                                              // argv address
		*esp = *esp - 4;
		*(uint32_t*) *esp = argv_address[i];
	}

	*esp = *esp - 4;
	*(uint32_t*) *esp = (uint32_t) *esp + 4;          // push argv

	*esp = *esp - 4;                                // push argc
	*(uint32_t*) *esp = count;

	*esp = *esp - 4;                                // push fake address
	*(uint32_t*) *esp = 0;

//  printf("esp : %x\n",*esp);

}

struct thread *get_child_process(int pid) {

	struct list_elem *elem_temp;
	struct thread *t_temp;
	// doubel linked list, find child process when have same pid
	for (elem_temp = list_begin(&thread_current()->child_list);
			elem_temp != list_end(&thread_current()->child_list); elem_temp =
					list_next(elem_temp)) {
		t_temp = list_entry(elem_temp, struct thread, child_elem);
		if (t_temp->tid == pid)             // find chlld process
			return t_temp;
	}
	return NULL;
}

void remove_child_process(struct thread *cp) {
	list_remove(&cp->child_elem);
	palloc_free_page(cp);                        // free struct of thread
}

int process_add_file(struct file *f) {
	struct thread *cur = thread_current();
	cur->fd_table[cur->fd_index] = f;
	cur->fd_index++;                             // next file fd index       
	return cur->fd_index - 1;                     // now file fd index
}

struct file *process_get_file(int fd) {
	struct thread *cur = thread_current();

	if (fd >= cur->fd_index || (cur->fd_table[fd] == NULL))
		return NULL;                        // no file for fd
	return cur->fd_table[fd];
}
void process_close_file(int fd) {
	struct file *f;
	if ((f = process_get_file(fd)) != NULL)       // wrong fd paramiter
	{
		file_close(f);
		thread_current()->fd_table[fd] = NULL;
	}

}
/* Waits for thread TID to die and returns its exit status.  If
 it was terminated by the kernel (i.e. killed due to an
 exception), returns -1.  If TID is invalid or if it was not a
 child of the calling process, or if process_wait() has already
 been successfully called for the given TID, returns -1
 immediately, without waiting.

 This function will be implemented in problem 2-2.  For now, it
 does nothing. */
int process_wait(tid_t child_tid UNUSED) {
	struct thread *t;
	int return_v = -1;

	t = get_child_process((int) child_tid);
	if (t == NULL)
		;
	else {

		sema_down(&t->exit_semaphore);  // wait parent 

		return_v = t->exit_status;      // in exit()
		remove_child_process(t);        // remove struct of thread

	}
	return return_v;

}

/* Free the current process's resources. */
void process_exit(void) {
	struct thread *cur = thread_current();
	uint32_t *pd;

	// struct file *f;
	int index = cur->fd_index;

	while (index > 2)                            // remove all of fd
	{
		process_close_file(index - 1);
		index--;
	}
	palloc_free_page(cur->fd_table);


	munmap(-1);
	vm_destroy(&cur->vm);

	/* Destroy the current process's page directory and switch back
	 to the kernel-only page directory. */
	pd = cur->pagedir;
	if (pd != NULL) {
		/* Correct ordering here is crucial.  We must set
		 cur->pagedir to NULL before switching page directories,
		 so that a timer interrupt can't switch back to the
		 process page directory.  We must activate the base page
		 directory before destroying the process's page
		 directory, or our active page directory will be one
		 that's been freed (and cleared). */
		cur->pagedir = NULL;
		pagedir_activate(NULL);
		pagedir_destroy(pd);
	}
}

/* Sets up the CPU for running user code in the current
 thread.
 This function is called on every context switch. */
void process_activate(void) {
	struct thread *t = thread_current();

	/* Activate thread's page tables. */
	pagedir_activate(t->pagedir);

	/* Set thread's kernel stack for use in processing
	 interrupts. */
	tss_update();
}

/* We load ELF binaries.  The following definitions are taken
 from the ELF specification, [ELF1], more-or-less verbatim.  */

/* ELF types.  See [ELF1] 1-2. */
typedef uint32_t Elf32_Word, Elf32_Addr, Elf32_Off;
typedef uint16_t Elf32_Half;

/* For use with ELF types in printf(). */
#define PE32Wx PRIx32   /* Print Elf32_Word in hexadecimal. */
#define PE32Ax PRIx32   /* Print Elf32_Addr in hexadecimal. */
#define PE32Ox PRIx32   /* Print Elf32_Off in hexadecimal. */
#define PE32Hx PRIx16   /* Print Elf32_Half in hexadecimal. */

/* Executable header.  See [ELF1] 1-4 to 1-8.
 This appears at the very beginning of an ELF binary. */
struct Elf32_Ehdr {
	unsigned char e_ident[16];
	Elf32_Half e_type;
	Elf32_Half e_machine;
	Elf32_Word e_version;
	Elf32_Addr e_entry;
	Elf32_Off e_phoff;
	Elf32_Off e_shoff;
	Elf32_Word e_flags;
	Elf32_Half e_ehsize;
	Elf32_Half e_phentsize;
	Elf32_Half e_phnum;
	Elf32_Half e_shentsize;
	Elf32_Half e_shnum;
	Elf32_Half e_shstrndx;
};

/* Program header.  See [ELF1] 2-2 to 2-4.
 There are e_phnum of these, starting at file offset e_phoff
 (see [ELF1] 1-6). */
struct Elf32_Phdr {
	Elf32_Word p_type;
	Elf32_Off p_offset;
	Elf32_Addr p_vaddr;
	Elf32_Addr p_paddr;
	Elf32_Word p_filesz;
	Elf32_Word p_memsz;
	Elf32_Word p_flags;
	Elf32_Word p_align;
};

/* Values for p_type.  See [ELF1] 2-3. */
#define PT_NULL    0            /* Ignore. */
#define PT_LOAD    1            /* Loadable segment. */
#define PT_DYNAMIC 2            /* Dynamic linking info. */
#define PT_INTERP  3            /* Name of dynamic loader. */
#define PT_NOTE    4            /* Auxiliary info. */
#define PT_SHLIB   5            /* Reserved. */
#define PT_PHDR    6            /* Program header table. */
#define PT_STACK   0x6474e551   /* Stack segment. */

/* Flags for p_flags.  See [ELF3] 2-3 and 2-4. */
#define PF_X 1          /* Executable. */
#define PF_W 2          /* Writable. */
#define PF_R 4          /* Readable. */

static bool setup_stack(void **esp);
static bool validate_segment(const struct Elf32_Phdr *, struct file *);
static bool load_segment(struct file *file, off_t ofs, uint8_t *upage,
		uint32_t read_bytes, uint32_t zero_bytes,
		bool writable);

/* Loads an ELF executable from FILE_NAME into the current thread.
 Stores the executable's entry point into *EIP
 and its initial stack pointer into *ESP.
 Returns true if successful, false otherwise. */
bool load(const char *file_name, void (**eip)(void), void **esp) {
	struct thread *t = thread_current();
	struct Elf32_Ehdr ehdr;
	struct file *file = NULL;
	off_t file_ofs;
	bool success = false;
	int i;

	/* Allocate and activate page directory. */
	t->pagedir = pagedir_create();
	if (t->pagedir == NULL)
		goto done;
	process_activate();

	/* Open executable file. */
	file = filesys_open(file_name);
	if (file == NULL) {
		printf("load: %s: open failed\n", file_name);
		goto done;
	}

	/* Read and verify executable header. */
	if (file_read(file, &ehdr, sizeof ehdr) != sizeof ehdr
			|| memcmp(ehdr.e_ident, "\177ELF\1\1\1", 7) || ehdr.e_type != 2
			|| ehdr.e_machine != 3 || ehdr.e_version != 1
			|| ehdr.e_phentsize != sizeof(struct Elf32_Phdr)
			|| ehdr.e_phnum > 1024) {
		printf("load: %s: error loading executable\n", file_name);
		goto done;
	}

	/* Read program headers. */
	file_ofs = ehdr.e_phoff;
	for (i = 0; i < ehdr.e_phnum; i++) {
		struct Elf32_Phdr phdr;

		if (file_ofs < 0 || file_ofs > file_length(file))
			goto done;
		file_seek(file, file_ofs);

		if (file_read(file, &phdr, sizeof phdr) != sizeof phdr)
			goto done;
		file_ofs += sizeof phdr;
		switch (phdr.p_type) {
		case PT_NULL:
		case PT_NOTE:
		case PT_PHDR:
		case PT_STACK:
		default:
			/* Ignore this segment. */
			break;
		case PT_DYNAMIC:
		case PT_INTERP:
		case PT_SHLIB:
			goto done;
		case PT_LOAD:
			if (validate_segment(&phdr, file)) {
				bool writable = (phdr.p_flags & PF_W) != 0;
				uint32_t file_page = phdr.p_offset & ~PGMASK;
				uint32_t mem_page = phdr.p_vaddr & ~PGMASK;
				uint32_t page_offset = phdr.p_vaddr & PGMASK;
				uint32_t read_bytes, zero_bytes;
				if (phdr.p_filesz > 0) {
					/* Normal segment.
					 Read initial part from disk and zero the rest. */
					read_bytes = page_offset + phdr.p_filesz;
					zero_bytes = (ROUND_UP(page_offset + phdr.p_memsz, PGSIZE)
							- read_bytes);
				} else {
					/* Entirely zero.
					 Don't read anything from disk. */
					read_bytes = 0;
					zero_bytes = ROUND_UP(page_offset + phdr.p_memsz, PGSIZE);
				}
				if (!load_segment(file, file_page, (void *) mem_page,
						read_bytes, zero_bytes, writable))
					goto done;
			} else
				goto done;
			break;
		}
	}

	/* Set up stack. */
	if (!setup_stack(esp))
		goto done;

	/* Start address. */
	*eip = (void (*)(void)) ehdr.e_entry;

	success = true;

	done:
	/* We arrive here whether the load is successful or not. */
	//file_close (file);
	return success;
}

/* load() helpers. */

//static bool install_page (void *upage, void *kpage, bool writable);
/* Checks whether PHDR describes a valid, loadable segment in
 FILE and returns true if so, false otherwise. */
static bool validate_segment(const struct Elf32_Phdr *phdr, struct file *file) {
	/* p_offset and p_vaddr must have the same page offset. */
	if ((phdr->p_offset & PGMASK) != (phdr->p_vaddr & PGMASK))
		return false;

	/* p_offset must point within FILE. */
	if (phdr->p_offset > (Elf32_Off) file_length(file))
		return false;

	/* p_memsz must be at least as big as p_filesz. */
	if (phdr->p_memsz < phdr->p_filesz)
		return false;

	/* The segment must not be empty. */
	if (phdr->p_memsz == 0)
		return false;

	/* The virtual memory region must both start and end within the
	 user address space range. */
	if (!is_user_vaddr((void *) phdr->p_vaddr))
		return false;
	if (!is_user_vaddr((void *) (phdr->p_vaddr + phdr->p_memsz)))
		return false;

	/* The region cannot "wrap around" across the kernel virtual
	 address space. */
	if (phdr->p_vaddr + phdr->p_memsz < phdr->p_vaddr)
		return false;

	/* Disallow mapping page 0.
	 Not only is it a bad idea to map page 0, but if we allowed
	 it then user code that passed a null pointer to system calls
	 could quite likely panic the kernel by way of null pointer
	 assertions in memcpy(), etc. */
	if (phdr->p_vaddr < PGSIZE)
		return false;

	/* It's okay. */
	return true;
}

/* Loads a segment starting at offset OFS in FILE at address
 UPAGE.  In total, READ_BYTES + ZERO_BYTES bytes of virtual
 memory are initialized, as follows:

 - READ_BYTES bytes at UPAGE must be read from FILE
 starting at offset OFS.

 - ZERO_BYTES bytes at UPAGE + READ_BYTES must be zeroed.

 The pages initialized by this function must be writable by the
 user process if WRITABLE is true, read-only otherwise.

 Return true if successful, false if a memory allocation error
 or disk read error occurs. */
static bool load_segment(struct file *file, off_t ofs, uint8_t *upage,
		uint32_t read_bytes, uint32_t zero_bytes, bool writable) {
	ASSERT((read_bytes + zero_bytes) % PGSIZE == 0);
	ASSERT(pg_ofs(upage) == 0);
	ASSERT(ofs % PGSIZE == 0);

	file_seek(file, ofs);
	while (read_bytes > 0 || zero_bytes > 0) {
		/* Calculate how to fill this page.
		 We will read PAGE_READ_BYTES bytes from FILE
		 and zero the final PAGE_ZERO_BYTES bytes. */
		size_t page_read_bytes = read_bytes < PGSIZE ? read_bytes : PGSIZE;
		size_t page_zero_bytes = PGSIZE - page_read_bytes;

		/* Get a page of memory. */
		struct vm_entry *v_entry;

		v_entry = malloc(sizeof(struct vm_entry)); //make virtual page system
		if (v_entry == NULL)
			return false;

		v_entry->vaddr = upage;             // initialize vme_entry member
		v_entry->type = VM_BIN;             // text, code section
		v_entry->writable = writable;
		v_entry->is_loaded = false;
		v_entry->pinned = false;
		v_entry->file = file;
		v_entry->offset = ofs;
		v_entry->read_bytes = page_read_bytes;
		v_entry->zero_bytes = page_zero_bytes;

		if(!insert_vme(&thread_current()->vm, v_entry))return false;

		/*      uint8_t *kpage = palloc_get_page (PAL_USER);
		 if (kpage == NULL)
		 return false;

		 Load this page.
		 if (file_read (file, kpage, page_read_bytes) != (int) page_read_bytes)
		 {
		 palloc_free_page (kpage);
		 return false;
		 }

		 memset (kpage + page_read_bytes, 0, page_zero_bytes);

		 Add the page to the process's address space.
		 if (!install_page (upage, kpage, writable))
		 {
		 palloc_free_page (kpage);
		 return false;
		 }
		 */
		/* Advance. */

		read_bytes -= page_read_bytes;
		zero_bytes -= page_zero_bytes;
		ofs += page_read_bytes;  // critical
		upage += PGSIZE;
	}
	return true;
}

/* Create a minimal stack by mapping a zeroed page at the top of
 user virtual memory. */
static bool setup_stack(void **esp) {
	struct page* page;
	uint8_t *kpage;
	bool success = false;

	struct vm_entry *v_entry;                          // make the vm_entry
//	kpage = palloc_get_page(PAL_USER | PAL_ZERO);
	page = alloc_page(PAL_USER);
	kpage = page->kaddr;

	void *virtual_address = ((uint8_t *) PHYS_BASE) - PGSIZE;
//  printf("esp = %x\n",virtual_address);
	if (kpage != NULL) {
		success = install_page(pg_round_down(virtual_address), kpage, true); // match vaddr -- physical memory

		if (success) {
			*esp = PHYS_BASE;             //starting of stack

		} else{
			free_page(kpage);
			return success;
		}
	}

	v_entry = malloc(sizeof(struct vm_entry));
	v_entry->vaddr = pg_round_down(virtual_address);
	v_entry->is_loaded = true;
	v_entry->writable = true;
	v_entry->type = VM_ANON;              //stack segment type
	v_entry->pinned = true;

	success = insert_vme(&thread_current()->vm, v_entry);

	page->vme = v_entry;
	return success;
}

/* Adds a mapping from user virtual address UPAGE to kernel
 virtual address KPAGE to the page table.
 If WRITABLE is true, the user process may modify the page;
 otherwise, it is read-only.
 UPAGE must not already be mapped.
 KPAGE should probably be a page obtained from the user pool
 with palloc_get_page().
 Returns true on success, false if UPAGE is already mapped or
 if memory allocation fails. */
static bool install_page(void *upage, void *kpage, bool writable) {
	struct thread *t = thread_current();

	/* Verify that there's not already a page at that virtual
	 address, then map our page there. */
	return (pagedir_get_page(t->pagedir, upage) == NULL
			&& pagedir_set_page(t->pagedir, upage, kpage, writable));
}
