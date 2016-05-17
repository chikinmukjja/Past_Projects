#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include <devices/shutdown.h>
#include <filesys/filesys.h>
#include "userprog/process.h"
#include "threads/synch.h"
#include <string.h>
#include <devices/input.h>
static void syscall_handler (struct intr_frame *);

void check_address(void *addr);
void get_argument(void *esp, int *arg, int count);

// system call function 
void halt(void);
void exit(int status);
bool create(const char *file, unsigned initial_size);
bool remove(const char *file);
tid_t exec(const char *cmd_line);
int wait(tid_t tid);
int open(const char *file);
int filesize(int fd);
int read(int fd, void *buffer, unsigned size);
int write(int fd, void *buffer, unsigned size);
void seek(int fd, unsigned position);
unsigned tell(int fd);
void close(int fd);

extern struct lock file_lock;   // lock for read , write

void
syscall_init (void) 
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

static void
syscall_handler (struct intr_frame *f UNUSED) 
{
  uint32_t *sp = f -> esp;
  check_address((void *)sp);               // check address of esp

  int syscall_n = *sp;                     // syscall_number
  int arg[5];                              // store argument 

  switch(syscall_n)
  {
	case SYS_HALT :                                    // system call 0 ~
		halt();	 
        case SYS_EXIT :                                             // exit
		get_argument(sp,arg,1);
		exit(arg[0]);
		break;
        case SYS_EXEC :                                 // make another process
		get_argument(sp,arg,1);
		check_address((void *)arg[0]);          // check string pointer
		f->eax = exec((const char *)arg[0]);
		break;
        case SYS_WAIT :                                
		get_argument(sp,arg,1);
		f->eax = wait(arg[0]);
		break; 
        case SYS_CREATE :                                //create new file
                get_argument(sp,arg,2);
		check_address((void *)arg[0]);
		f->eax = create((const char *)arg[0],arg[1]);
		break;
	case SYS_REMOVE :                                // remove file
		get_argument(sp,arg,1);
		check_address((void *)arg[0]);
		f->eax = remove((const char *)arg[0]);
		break;
	case SYS_OPEN :                              
		get_argument(sp,arg,1);
		check_address((void *)arg[0]);
		f->eax = open((const char *)arg[0]);     // return fd, not -1
		break;
	case SYS_FILESIZE :
		get_argument(sp,arg,1);
		f->eax = filesize(arg[0]);
		break;
	case SYS_READ  :                    // argument ( fd ,buffer ,size )
		get_argument(sp,arg,3);
		check_address((void *)arg[1]);
		f->eax = read(arg[0],(void *)arg[1],arg[2]);
		break;
 	case SYS_WRITE :                    // argument ( fd ,buffer ,size )
		get_argument(sp,arg,3);
		check_address((void *)arg[1]);
		f->eax = write(arg[0],(void *)arg[1],arg[2]);
		break;
	case SYS_SEEK  :
		get_argument(sp,arg,2);
		seek(arg[0],arg[1]);
		break;
        case SYS_TELL  :
		get_argument(sp,arg,1);
		tell(arg[0]);
		break;    
	case SYS_CLOSE :
		get_argument(sp,arg,1);
		close(arg[0]);
		break;
	default :
	//	thread_exit(); 
		break;
  }
// printf ("system call!\n");
//  thread_exit ();
}

void check_address(void *addr)
{
	if( 0x8048000 <(uint32_t *)addr &&(uint32_t *)addr < 0xc0000000 );
//	printf("check addr : %x\n",(uint32_t *)addr); 
						     // check user domain
	else exit(-1);                                       // process exit 
}

void get_argument(void *esp, int *arg, int count)
{
	int i=count;
	void *pointer = esp +4;            // above system call number
	
	for(i = 0; i < count; i++)
	{
		arg[i] = *(int *)pointer;
		pointer = pointer +4;
		check_address(pointer);          
	}
	esp = esp + 4*(count+1);           // change esp poiter location
}

void halt()
{
	shutdown_power_off();
}

void exit(int status)
{
	thread_current()->exit_status = status;             //store exit status
	printf("%s: exit(%d)\n",thread_current()->name,status);
        thread_exit();
}

bool create(const char *file, unsigned initial_size)
{
	return filesys_create(file,initial_size);
}

bool remove(const char *file)
{
	return filesys_remove(file);
}

tid_t exec(const char *cmd_line)
{
	tid_t pid;
	struct thread *t;
	
	pid = process_execute(cmd_line);       // create child process
	t = get_child_process((int)pid);       // find child process
	
 	sema_down(&t->load_semaphore);         // wait child process load
	
	if(t->process_load)return pid;
	else return -1;
	
}

int wait(tid_t tid)
{
	return process_wait(tid);	
} 

int open(const char *file)
{
	struct file *f;
	
	if((f = filesys_open(file)) == NULL)
	return -1;
        else return process_add_file(f);

}
int filesize(int fd)
{
	struct file *f;
	if((f = process_get_file(fd)) == NULL)
		return -1;
	else return file_length(f);
}  

int read(int fd, void *buffer, unsigned size)
{
	unsigned count = 0;
	int read_size = -1;
	struct file *f;

	lock_acquire(&file_lock);

	if(fd == 1 || fd < 0);                       // handle exception 
	else if(fd == 0)                             // stdin 
	{
		for(count=0;count<size;count++)
				((char *)buffer)[count] = input_getc();
		read_size = count;
	}
	else 
	{
		if((f= process_get_file(fd)) != NULL)
			read_size = file_read(f,buffer,size);	
	}
	
	lock_release(&file_lock);
	return read_size;


} 
int write(int fd, void *buffer, unsigned size)
{
	struct file *f;
	unsigned write_size=-1;

	lock_acquire(&file_lock);     // lock to write alone
	
	if(fd < 1);                   // exception  return size 0;
	else if(fd == 1)              // write on stdout 
	{
		putbuf((char *)buffer,size);
		write_size = size;
	}
	else
	{
		if((f=process_get_file(fd)) != NULL )
			write_size = file_write(f,buffer,size);
	}
	lock_release(&file_lock);
	return write_size;
} 

void seek(int fd, unsigned position)
{
	struct file *f;
	if((f = process_get_file(fd)) != NULL)
		file_seek(f,position);
	
}
unsigned tell(int fd)
{
	struct file *f;
	if((f = process_get_file(fd)) !=  NULL)
		return (unsigned)file_tell(f);
	else return 0;
		
}

void close(int fd)
{
	process_close_file(fd);
}
